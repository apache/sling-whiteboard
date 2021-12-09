// the module 'vscode' contains the VS Code extensibility API
import * as vscode from 'vscode';
// import our own completion provider
import { HtlCompletionItemProvider } from './htlCompletionItemProvider';

// this method is called when your extension is activated
// your extension is activated the very first time the command is executed
export function activate(context: vscode.ExtensionContext) {
	let completionsPath = vscode.Uri.joinPath(context.extensionUri, "data");
	let disposable = vscode.languages.registerCompletionItemProvider('html', new HtlCompletionItemProvider(completionsPath));
	context.subscriptions.push(disposable);
}

// this method is called when your extension is deactivated
export function deactivate() {}
