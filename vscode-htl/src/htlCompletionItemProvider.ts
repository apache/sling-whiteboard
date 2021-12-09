'use strict';

import * as vscode from 'vscode';
// HTML parser module used to provide context-sensitive completion
import { parse } from 'node-html-parser';
import { readFileSync } from 'fs';
import {CompletionDataAccess, CompletionDefinition, LocalCompletionDefinition} from './completionData';

const slyUseRegexp = /data-sly-use\.([a-zA-Z0-9]+)=/g;
const identifierAccess = /([a-zA-Z0-9]+)\./g;

export class HtlCompletionItemProvider implements vscode.CompletionItemProvider {

    completionData: CompletionDataAccess;

    constructor(completionsPath: vscode.Uri) {
        const slingCompletions = vscode.Uri.joinPath(completionsPath, "completions-sling.json");
        this.completionData = new CompletionDataAccess(JSON.parse(readFileSync(slingCompletions.fsPath, 'utf-8')));
        
    }
           
    provideCompletionItems(document: vscode.TextDocument, position: vscode.Position, token: vscode.CancellationToken, context: vscode.CompletionContext) {
        return this.provideCompletionItems0(document.getText(new vscode.Range(position.with(undefined, 0), position)), document.getText());
    }

    provideCompletionItems0(linePrefix: string, doc: string) {
        let completionStart = linePrefix.indexOf('${');
        if ( completionStart === -1 ) {
            return null;
        }
        
        let completionContext = linePrefix.substring(completionStart + 2).trim();

        // 1. propose completions based on HTML document
        let documentCompletions: CompletionDefinition[] = [];
        let htmlDoc = parse(doc);
        let elements = htmlDoc.getElementsByTagName("*");
        // TODO - provide only relevant completions based on the position in the document
        elements
            .filter( e => e.rawAttrs.indexOf('data-sly-') >= 0 )
            .forEach(e => {
                // element.attributes parses data-sly-use.foo="bar" incorrectly into {data-sly-use="", foo="bar"}
                let rawAttrs = e.rawAttrs;
                for ( const match of rawAttrs.matchAll(slyUseRegexp) ) {
                    documentCompletions.push(new LocalCompletionDefinition(match[1], "java.lang.Object", ""));
                }
                if ( rawAttrs.indexOf('data-sly-repeat=') >= 0 || rawAttrs.indexOf('data-sly-list') >= 0)  {
                    // TODO - resolve item if possible
                    documentCompletions.push(new LocalCompletionDefinition("item", "java.lang.Object", ""));
                    documentCompletions.push(new LocalCompletionDefinition("itemList", "$io.sightly.ItemList", ""));
                }
                // TODO - support named data-sly-repeat completions, e.g. data-sly-repeat.meh=...
            });

        let completionProperties = this.completionData.getGlobalCompletions().concat(documentCompletions);

        let completionCandidate = "";

        // 2. recursively resolve any nested properties
        for ( const match of completionContext.matchAll(identifierAccess)) {
            completionCandidate = match[1];
            let matchingDefinition = completionProperties.find( e => e.name === completionCandidate );
            if ( matchingDefinition ) {
                completionProperties = this.completionData.findPropertyCompletions(matchingDefinition.javaType);
            } else {
                completionProperties = [];
                break;
            }
        }

        // provide completions based on properties ( top-level bindings or nested ones)
        return completionProperties.map ( element =>  this.toCompletionItem(element) );
    }

    private toCompletionItem(completionDefinition: CompletionDefinition) {
        let item = new vscode.CompletionItem(completionDefinition.name);
        let description = "";
        if ( completionDefinition.description ) {
            description = completionDefinition.description + "\n\n";
        }

        // filter out synthetic types
        if ( completionDefinition.javaType.charAt(0) !== '$') {
            description += "Type: _" + completionDefinition.javaType+"_";
        }
        item.documentation = new vscode.MarkdownString(description);
        
        return item;
        
    }
}