# Visual Studio code extension for HTL

Extension that provides HTL support for Visual Studio Code.

Supported features:

- custom tags ( `sly` )
- custom tag attributes ( `data-sly-*` )
- expression language completions
    - default Sling bindings: `resource`, `request`, etc
    - deep auto-completion support based on inferred type: `request.resource.parent`
    - auto-completion for bindings exposed from `data-sly-use`
    - auto-completion for bindings exposed from `data-sly-list` and `data-sly-repeat` ( `item`, `itemList` )
