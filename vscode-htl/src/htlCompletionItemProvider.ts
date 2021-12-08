'use strict';

import * as vscode from 'vscode';
// HTML parser module used to provide context-sensitive completion
import { parse } from 'node-html-parser';
import { readFileSync } from 'fs';

const slyUseRegexp = /data-sly-use\.([a-zA-Z0-9]+)=/g;

export class HtlCompletionItemProvider implements vscode.CompletionItemProvider {

    completions: any;

    constructor(completionsPath: vscode.Uri) {
        const slingCompletions = vscode.Uri.joinPath(completionsPath, "completions-sling.json");
        console.log("Reading completions from {}", slingCompletions.fsPath);
        this.completions = JSON.parse(readFileSync(slingCompletions.fsPath, 'utf-8'));
        
    }
           
    provideCompletionItems(document: vscode.TextDocument, position: vscode.Position, token: vscode.CancellationToken, context: vscode.CompletionContext) {
        return this.provideCompletionItems0(document.getText(new vscode.Range(position.with(undefined, 0), position)), document.getText());
    }

    provideCompletionItems0(linePrefix: string, doc: string) {
        if ( linePrefix.indexOf('${') === -1 ) {
            return null;
        }
        // request-specific branch
        if ( linePrefix.endsWith('request.') ) {
            return [
                new vscode.CompletionItem('resource'),
                new vscode.CompletionItem('resourceResolver'),
                new vscode.CompletionItem('requestPathInfo'),
                new vscode.CompletionItem('contextPath')
            ];
        } else {

            let generalCompletions: vscode.CompletionItem[] = [];

            this.completions.globalCompletions.forEach( (globalCompletion: any) => {
                let vsCodeCompletion = new vscode.CompletionItem(globalCompletion.name);
                if ( globalCompletion.description ) {
                    vsCodeCompletion.documentation = new vscode.MarkdownString(globalCompletion.description);
                }
                generalCompletions.push(vsCodeCompletion);
            });

            let htmlDoc = parse(doc);
            let elements = htmlDoc.getElementsByTagName("*");
            // TODO - provide only relevant completions based on the position in the document
            elements
                .filter( e => e.rawAttrs.indexOf('data-sly-') >= 0 )
                .forEach(e => {
                    // element.attributes parses data-sly-use.foo="bar" incorrectly into {data-sly-use="", foo="bar"}
                    let rawAttrs = e.rawAttrs;
                    for ( const match of rawAttrs.matchAll(slyUseRegexp) ) {
                        generalCompletions.push(new vscode.CompletionItem(match[1]));
                    }
                    if ( rawAttrs.indexOf('data-sly-repeat=') >= 0 )  {
                        generalCompletions.push(new vscode.CompletionItem("item"));
                        generalCompletions.push(new vscode.CompletionItem("itemList")); // TODO - expand completions for itemList
                    }
                    // TODO - support named data-sly-repeat completions, e.g. data-sly-repeat.meh=...
                });

            return generalCompletions;
        }
    }
}