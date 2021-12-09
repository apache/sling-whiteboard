'use strict';

import * as vscode from 'vscode';
// HTML parser module used to provide context-sensitive completion
import { parse } from 'node-html-parser';
import { readFileSync } from 'fs';
import {CompletionDataAccess, CompletionDefinition} from './completionData';
import { cpuUsage } from 'process';

const slyUseRegexp = /data-sly-use\.([a-zA-Z0-9]+)=/g;
const identifierAccess = /([a-zA-Z0-9]+)\./g;

export class HtlCompletionItemProvider implements vscode.CompletionItemProvider {

    completionData: CompletionDataAccess;

    constructor(completionsPath: vscode.Uri) {
        const slingCompletions = vscode.Uri.joinPath(completionsPath, "completions-sling.json");
        console.log("Reading completions from {}", slingCompletions.fsPath);
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
        let completions: vscode.CompletionItem[] = [];
        let completionCandidate = "";
        let previousJavaType = "";

        for ( const match of completionContext.matchAll(identifierAccess)) {
            let completionProperties: CompletionDefinition[];
            if ( previousJavaType ) {
                completionProperties = this.completionData.findPropertyCompletions(previousJavaType);
            }  else {
                completionProperties = this.completionData.getGlobalCompletions();
            }
            completionCandidate = match[1];
            let matchingDefinition = completionProperties.find( e => e.name === completionCandidate );
            if ( matchingDefinition ) {
                previousJavaType = matchingDefinition.javaType;
            }
        }

        if ( completionCandidate ) {
            let completionProposals = this.completionData.findPropertyCompletions(previousJavaType);

            completionProposals.forEach ( element => {
                completions.push( this.toCompletionItem(element) );
            });
        } else {

            this.completionData.getGlobalCompletions().map( element => {
                completions.push(this.toCompletionItem(element));
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
                        completions.push(new vscode.CompletionItem(match[1]));
                    }
                    if ( rawAttrs.indexOf('data-sly-repeat=') >= 0 )  {
                        completions.push(new vscode.CompletionItem("item"));
                        completions.push(new vscode.CompletionItem("itemList")); // TODO - expand completions for itemList
                    }
                    // TODO - support named data-sly-repeat completions, e.g. data-sly-repeat.meh=...
                });
        }

        return completions;
    }

    private toCompletionItem(completionDefintion: CompletionDefinition) {
        let item = new vscode.CompletionItem(completionDefintion.name);
        if ( completionDefintion.description ) {
            item.documentation = new vscode.MarkdownString(completionDefintion.description);
        }
        return item;
        
    }
}