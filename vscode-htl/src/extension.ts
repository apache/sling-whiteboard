// the module 'vscode' contains the VS Code extensibility API
import * as vscode from 'vscode';
// import our own completion provider
import { HtlCompletionItemProvider } from './htlCompletionItemProvider';

// this method is called when your extension is activated
// your extension is activated the very first time the command is executed
export function activate(context: vscode.ExtensionContext) {
	
	// Use the console to output diagnostic information (console.log) and errors (console.error)
	// This line of code will only be executed once when your extension is activated
	console.log('Congratulations, your extension "vscode-hello" is now active!');

	// The command has been defined in the package.json file
	// Now provide the implementation of the command with registerCommand
	// The commandId parameter must match the command field in package.json
	let helloWorld = vscode.commands.registerCommand('vscode-hello.helloWorld', () => {
		vscode.window.showErrorMessage('Hello VS Code');
	});

	let now = vscode.commands.registerCommand('vscode-hello.now', () => {
		vscode.window.showWarningMessage(new Date().toISOString());
	});

	context.subscriptions.push(helloWorld, now);

	let completionsPath = vscode.Uri.joinPath(context.extensionUri, "data");

	vscode.languages.registerCompletionItemProvider('html', new HtlCompletionItemProvider(completionsPath));
}

// this method is called when your extension is deactivated
export function deactivate() {}
