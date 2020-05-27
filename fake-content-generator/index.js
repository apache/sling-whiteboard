// Generate fake content in the Sling initial content JSON format
// Using faker.js (https://github.com/marak/Faker.js/)

// To run this do "npm install" and then "node index.js" to run it

const faker = require('faker');
const fs = require('fs');

const allTags = new Set();
for(i=0 ; i < 60; i++) {
  allTags.add(faker.hacker.noun());
}

const allFilenames = new Set();

const folders = [
  "News",
  "Business",
  "Culture",
  "Music",
  "Adventure",
  "Travel"
]

function randomInt(max) {
    return Math.floor(Math.random() * (max + 1));
}

function randomFromSet(input, maxValues) {
  const result = [];
  var nValues = randomInt(maxValues)
  input.forEach(it => {
    if(nValues-- >= 0 && Math.random() > 0.5) {
      result.push(it);
    }
  });
  return result;
}

function generatePage() {
  const folder = folders[randomInt(folders.length - 1)];
  const name = faker.fake("{{name.firstName}} {{name.lastName}}");
  const title = `${name} ${faker.fake("on the {{hacker.noun}} of {{hacker.adjective}} '{{lorem.words(2)}}' (aka {{hacker.abbreviation}})")}`;
  const filename = faker.helpers.slugify(title).toLowerCase();
  
  allFilenames.add(filename);
  
  return {
    source: "Apache Sling's fake-content-generator",
    section: folder,
    folder: folder.toLowerCase(),
    filename: filename,
    title: `${folder} - ${title}`,
    tags: randomFromSet(allTags, 5),
    seeAlso: randomFromSet(allFilenames, 7),
    text: `As ${name} often says, ${faker.lorem.paragraphs(randomInt(12), "<br/>\n")}`,
  }
}

function mkDirIfNeeded(path, callback) {
  if(!fs.existsSync(path)) {
    fs.mkdirSync(path);
    if(callback) {
      callback(path);
    }
  }
}

const nFiles = 1000;
const baseOutputFolder = "./output";

function setupCategoryFolder(path, name, section) {
  mkDirIfNeeded(path, path => {
    // Define Sling resource properties for the created folder
    const output = `${baseOutputFolder}/${name}.json`;
    const props = {};
    props["jcr:primaryType"] = "sling:Folder";
    props["sling:resourceType"] = `samples/section`;
    props["name"] = section;
    fs.writeFile(output, JSON.stringify(props), err => { if(err) throw err; });
  });
}

console.log(`Generating ${nFiles} fake content files under ${baseOutputFolder}`);
mkDirIfNeeded(baseOutputFolder);

for(i=0 ; i < nFiles; i++) {
  const page = generatePage();
  page["jcr:primaryType"] = "nt:unstructured";
  page["sling:resourceType"] = `samples/article/${page.folder}`;
  page["sling:resourceSuperType"] = "samples/article";
  const outputFolder = `${baseOutputFolder}/${page.folder}`;
  setupCategoryFolder(outputFolder, page.folder, page.section);
  
  const output = `${outputFolder}/${page.filename}.json`;
  fs.writeFile(output, JSON.stringify(page), err => { if(err) throw err; });
}
