'use strict';

import * as vscode from 'vscode';

export class HtlCompletionItemProvider implements vscode.CompletionItemProvider {
    
    public provideCompletionItems(document: vscode.TextDocument, position: vscode.Position, token: vscode.CancellationToken, context: vscode.CompletionContext): vscode.ProviderResult<vscode.CompletionItem[] | vscode.CompletionList<vscode.CompletionItem>> {
        throw new Error('Method not implemented.');
    }
}