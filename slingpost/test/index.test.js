/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
const fetch = require("jest-fetch-mock");
jest.setMock("node-fetch", fetch);

const SlingPost = require("../index");

beforeEach(() => {
  fetch.resetMocks();
});

describe("testing Sling Post Client", () => {
  test("Construct a default Sling Post instance", () => {
    const sp = new SlingPost();
    expect(sp.config.url).toBe("http://localhost:8080");
    expect(sp.config.username).toBe("admin");
    expect(sp.config.password).toBe("admin");
    expect(sp.config.base).toBe("./dist");
  });

  test("Construct a configured Sling Post instance", () => {
    const sp = new SlingPost({
      url: "http://localhost:9080",
      password: "admin1",
    });
    expect(sp.config.url).toBe("http://localhost:9080");
    expect(sp.config.username).toBe("admin");
    expect(sp.config.password).toBe("admin1");
    expect(sp.config.base).toBe("./dist");
  });
});

describe("automatic properties", () => {
  test("Testing adding automatic properties", () => {
    const res = SlingPost.addAutomaticProperties({
      testprop: 2,
      "jcr:createdBy": "tony",
    });
    expect(res["jcr:createdBy"]).toBe("tony");
    expect(res.testprop).toBe(2);
    expect(res["jcr:created"]).toBe("");
    expect(res["jcr:lastModified"]).toBe("");
    expect(res["jcr:lastModifiedBy"]).toBe("");
  });
});

describe("repository path", () => {
  test("repositoryPath replaces the base path to generate the repository path", () => {
    const sp = new SlingPost();
    const inscope = sp.repositoryPath("./dist/test.js");
    expect(inscope).toBe("/test.js");
  });

  test("repositoryPath won't replace paths outside the base path to generate the repository path", () => {
    const sp = new SlingPost();
    const inscope = sp.repositoryPath("./src/test.js");
    expect(inscope).not.toBe("/test.js");
  });
});

describe("copy", () => {
  test("copy sends expected parameters", async () => {
    const sp = new SlingPost();

    let body = "";
    fetch.mockResponse((req) => {
      body = JSON.stringify(req.body);
      return Promise.resolve("Success");
    });
    await sp.copy("/src", "/ed");

    expect(fetch).toBeCalled();
    expect(fetch.mock.calls[0][0]).toEqual("http://localhost:8080/src");
    expect(body).toEqual(
      expect.stringContaining('name=\\":dest\\"\\r\\n\\r\\n","/ed"')
    );
    expect(body).toEqual(
      expect.stringContaining('name=\\":operation\\"\\r\\n\\r\\n","copy"')
    );
  });
});

describe("delete", () => {
  test("delete sends expected parameters", async () => {
    const sp = new SlingPost();

    let body = "";
    fetch.mockResponse((req) => {
      body = JSON.stringify(req.body);
      return Promise.resolve("Success");
    });
    await sp.delete("/src");

    expect(fetch).toBeCalled();
    expect(fetch.mock.calls[0][0]).toEqual("http://localhost:8080/src");
    expect(body).toEqual(
      expect.stringContaining('name=\\":operation\\"\\r\\n\\r\\n","delete"')
    );
  });
});

describe("import content", () => {
  test("import content sends expected defaults", async () => {
    const sp = new SlingPost({ base: "." });

    let body = "";
    fetch.mockResponse((req) => {
      body = JSON.stringify(req.body);
      return Promise.resolve("Success");
    });
    await sp.importContent("MY_CONTENT", "/import", "contentname");

    expect(fetch).toBeCalled();
    expect(fetch.mock.calls[0][0]).toEqual("http://localhost:8080/import");
    expect(body).toEqual(
      expect.stringContaining('name=\\":operation\\"\\r\\n\\r\\n","import"')
    );
    expect(body).toEqual(
      expect.stringContaining('name=\\":content\\"\\r\\n\\r\\n","MY_CONTENT"')
    );
    expect(body).toEqual(
      expect.stringContaining('name=\\":contentType\\"\\r\\n\\r\\n","json"')
    );
    expect(body).toEqual(
      expect.stringContaining('name=\\\":name\\\"\\r\\n\\r\\n\",\"contentname\"')
    );
  });

  test("import content sends expected parameters", async () => {
    const sp = new SlingPost({ base: "." });

    let body = "";
    fetch.mockResponse((req) => {
      body = JSON.stringify(req.body);
      return Promise.resolve("Success");
    });
    await sp.importContent("MY_CONTENT", "/import", "name", "xml", false, false);

    expect(fetch).toBeCalled();
    expect(fetch.mock.calls[0][0]).toEqual("http://localhost:8080/import");
    expect(body).toEqual(
      expect.stringContaining('name=\\":operation\\"\\r\\n\\r\\n","import"')
    );
    expect(body).toEqual(
      expect.stringContaining('name=\\":content\\"\\r\\n\\r\\n","MY_CONTENT"')
    );
    expect(body).toEqual(
      expect.stringContaining('name=\\":contentType\\"\\r\\n\\r\\n","xml"')
    );
    expect(body).toEqual(
      expect.not.stringContaining('name=\\\":replace\\\"\\r\\n\\r\\n\",\"true\"')
    );
    expect(body).toEqual(
      expect.not.stringContaining('name=\\\":replaceProperties\\\"\\r\\n\\r\\n\",\"true\"')
    );
    expect(body).toEqual(
      expect.stringContaining('name=\\\":name\\\"\\r\\n\\r\\n\",\"name\"')
    );
  });
});

describe("import file", () => {
  test("import file sends expected defaults", async () => {
    const sp = new SlingPost({ base: "." });

    let body = "";
    fetch.mockResponse((req) => {
      body = JSON.stringify(req.body);
      return Promise.resolve("Success");
    });
    await sp.importFile("./test/test.json");

    expect(fetch).toBeCalled();
    expect(fetch.mock.calls[0][0]).toEqual("http://localhost:8080/test");
    expect(body).toEqual(
      expect.stringContaining('name=\\":operation\\"\\r\\n\\r\\n","import"')
    );
    expect(body).toEqual(
      expect.stringContaining(
        'name=\\":contentFile\\"; filename=\\"test.json\\"'
      )
    );
    expect(body).toEqual(
      expect.stringContaining('name=\\":contentType\\"\\r\\n\\r\\n","json"')
    );
    expect(body).toEqual(
      expect.stringContaining('name=\\":name\\"\\r\\n\\r\\n","test"')
    );
  });

  test("import file handles a glob", async () => {
    const sp = new SlingPost();
    fetch.mockResponse("Success");
    await sp.importFile("./test/test*.json");
    expect(fetch.mock.calls.length).toEqual(2);
  });

  test("import file sends expected parameters", async () => {
    const sp = new SlingPost({ base: "." });

    let body = "";
    fetch.mockResponse((req) => {
      body = JSON.stringify(req.body);
      return Promise.resolve("Success");
    });
    await sp.importFile("./test/test.json", "/import", "test2", "xml", false, false);

    expect(fetch).toBeCalled();
    expect(fetch.mock.calls[0][0]).toEqual("http://localhost:8080/import");
    expect(body).toEqual(
      expect.stringContaining('name=\\":operation\\"\\r\\n\\r\\n","import"')
    );
    expect(body).toEqual(
      expect.stringContaining(
        'name=\\":contentFile\\"; filename=\\"test.json\\"'
      )
    );
    expect(body).toEqual(
      expect.stringContaining('name=\\":contentType\\"\\r\\n\\r\\n","xml"')
    );
    expect(body).toEqual(
      expect.not.stringContaining('name=\\\":replace\\\"\\r\\n\\r\\n\",\"true\"')
    );
    expect(body).toEqual(
      expect.not.stringContaining('name=\\\":replaceProperties\\\"\\r\\n\\r\\n\",\"true\"')
    );
    expect(body).toEqual(
      expect.stringContaining('name=\\\":name\\\"\\r\\n\\r\\n\",\"test2\"')
    );
  });
});

describe("move", () => {
  test("move sends expected parameters", async () => {
    const sp = new SlingPost();

    let body = "";
    fetch.mockResponse((req) => {
      body = JSON.stringify(req.body);
      return Promise.resolve("Success");
    });
    await sp.move("/src", "/ed");

    expect(fetch).toBeCalled();
    expect(fetch.mock.calls[0][0]).toEqual("http://localhost:8080/src");
    expect(body).toEqual(
      expect.stringContaining('name=\\":dest\\"\\r\\n\\r\\n","/ed"')
    );
    expect(body).toEqual(
      expect.stringContaining('name=\\":operation\\"\\r\\n\\r\\n","move"')
    );
  });
});

describe("post", () => {
  test("post sends expected parameters", async () => {
    const sp = new SlingPost();

    let body = "";
    fetch.mockResponse((req) => {
      body = JSON.stringify(req.body);
      return Promise.resolve("Success");
    });
    await sp.post("/test", {
      param1: "value1",
    });

    expect(fetch).toBeCalled();
    expect(fetch.mock.calls[0][0]).toEqual("http://localhost:8080/test");
    expect(body).toEqual(
      expect.stringContaining('name=\\"param1\\"\\r\\n\\r\\n","value1"')
    );
  });

  test("post throws exception on failure", async () => {
    const sp = new SlingPost();

    try {
      fetch.mockReject("No Good!");
      await sp.post("/test", {
        param1: "value1",
      });
    } catch (e) {
      expect(e).toEqual("No Good!");
    }
  });

  test("post throws exception on invalid response", async () => {
    const sp = new SlingPost();

    try {
      fetch.mockResponse(JSON.stringify([{ status: "sad" }]), { status: 500 });
      await sp.post("/test", {
        param1: "value1",
      });
    } catch (e) {
      expect(e).toEqual(
        "Failed with invalid status: 500 - Internal Server Error"
      );
    }
  });
});

describe("uploadFile", () => {
  test("uploadFile sends expected parameters", async () => {
    const sp = new SlingPost({ base: "." });

    fetch.mockResponse((req) => {
      return Promise.resolve("Success");
    });
    await sp.uploadFile("./test/test.json");

    expect(fetch).toBeCalled();
    expect(fetch.mock.calls[0][0]).toEqual("http://localhost:8080/test");
  });

  test("uploadFile sends provided parameters", async () => {
    const sp = new SlingPost({ base: "." });

    let body = "";
    fetch.mockResponse((req) => {
      body = JSON.stringify(req.body);
      return Promise.resolve("Success");
    });
    await sp.uploadFile("./test/test.json", null, {
      param1: "value1",
    });

    expect(fetch).toBeCalled();
    expect(fetch.mock.calls[0][0]).toEqual("http://localhost:8080/test");
    expect(body).toEqual(
      expect.stringContaining('name=\\"param1\\"\\r\\n\\r\\n","value1"')
    );
  });

  test("uploadFile sends provided path", async () => {
    const sp = new SlingPost({ base: "." });

    fetch.mockResponse((req) => {
      return Promise.resolve("Success");
    });
    await sp.uploadFile("./test/test.json", "/test2");

    expect(fetch).toBeCalled();
    expect(fetch.mock.calls[0][0]).toEqual("http://localhost:8080/test2");
  });

  test("uploadFile sends provided path and parameters", async () => {
    const sp = new SlingPost({ base: "." });

    let body = "";
    fetch.mockResponse((req) => {
      body = JSON.stringify(req.body);
      return Promise.resolve("Success");
    });
    await sp.uploadFile("./test/test.json", "/test2", {
      param1: "value1",
    });

    expect(fetch).toBeCalled();
    expect(fetch.mock.calls[0][0]).toEqual("http://localhost:8080/test2");
    expect(body).toEqual(
      expect.stringContaining('name=\\"param1\\"\\r\\n\\r\\n","value1"')
    );
  });

  test("uploadFile throws exception on failure", async () => {
    const sp = new SlingPost();

    try {
      fetch.mockReject("No Good!");
      await sp.uploadFile("./test/test.json", "/test2", {
        param1: "value1",
      });
    } catch (e) {
      expect(e).toEqual("No Good!");
    }
  });

  test("uploadFile throws exception on invalid response", async () => {
    const sp = new SlingPost();

    try {
      fetch.mockResponse(JSON.stringify([{ status: "sad" }]), { status: 500 });
      await sp.uploadFile("./test/test.json", "/test2", {
        param1: "value1",
      });
    } catch (e) {
      expect(e).toEqual(
        "Failed with invalid status: 500 - Internal Server Error"
      );
    }
  });

  test("uploadFile handles a glob", async () => {
    const sp = new SlingPost();
    fetch.mockResponse("Success");
    await sp.uploadFile("./test/test*.json");
    expect(fetch.mock.calls.length).toEqual(2);
  });
});
