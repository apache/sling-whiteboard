[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://www.apache.org/licenses/LICENSE-2.0)

# TagStream
Module to provide a fast, HTML5/XML tokenzier. TagStream takes an InputStream and tokenizes into a Tag or Text element which can be used for inspection or transformation by leveraging Java's Stream API.

This provides:
* Reduced memory footprint by eliminating the virtual dom
* Event based/pull processing of the source
* Enables stream processing from an input to an output stream


## Using TagStream 
There are multiple ways which provides access to the processing of an HTML/XML source. 

### Iteration
The ``TagIterator`` allows you to iterate the HTML/XML document utilizing a pull methodology. Whenever you request the next element, the element is tokenized from the InputStream

### Stream
The ``Tag`` class wraps the ``TagIterator`` to provide a ``Stream<Element>`` provider.


Examples:

```java
//count the number of start tags
Tag.stream(inputStream).filter(elem -> elem.getType() == ElementType.START_TAG ).count();
```

```java
// find any elements that has an href attribute that doesn't point anywhere
// print out the bad links as an html list of anchors 
stream.filter(elem -> elem.getType() == ElementType.START_TAG && elem.hasAttribute("href") )
    .filter(elem -> isLinkBad(elem.getAttribute("href")))
    .map(HtmlStreams.TO_HTML)
    .forEach(System.out::println);
```

```java
//count the number of tags
HtmlSAXSupport saxEventGenerator = new HtmlSAXSupport(customHandler);
Tag.stream(inputStream).forEach(saxEventGenerator);
```
TagStream works by using the W3C's HTML5 *parsing* rules to properly identify a Tag. This is a separate set of guidelines from what defines valid HTML. The TagStream parser responds with a Tag or Text element and then proceeds to the next section. It does not attempt to create a DOM tree, it doesn't perform validation of tag it found. It assumes that you know what you are doing and won't judge you.


