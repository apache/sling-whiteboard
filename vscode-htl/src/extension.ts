// The module 'vscode' contains the VS Code extensibility API
// Import the module and reference it with the alias vscode in your code below
import * as vscode from 'vscode';

// HTML parser module used to provide context-sensitive completion
import { parse } from 'node-html-parser';

const slyUseRegexp = /data-sly-use\.([a-zA-Z0-9]+)=/g;

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

	vscode.languages.registerCompletionItemProvider('html', {
		provideCompletionItems(document: vscode.TextDocument, position: vscode.Position, token: vscode.CancellationToken, context: vscode.CompletionContext) {
			
			let line = document.lineAt(position);
			let lineUntilPosition = document.getText(new vscode.Range(position.with(undefined, 0), position));
			let lineAfterPosition = document.getText(new vscode.Range(position, position.with(undefined, line.text.length)));
			if ( lineUntilPosition.indexOf('${') === -1 ) {
				return null;
			}
			// request-specific branch
			if ( lineUntilPosition.endsWith('request.') ) {
				return [
					new vscode.CompletionItem('resource'),
					new vscode.CompletionItem('resourceResolver'),
					new vscode.CompletionItem('requestPathInfo'),
					new vscode.CompletionItem('contextPath')
				];
			} else {

				let generalCompletions = [];

				// TODO - provide completions for all global bindings
				let props = new vscode.CompletionItem('properties');
				props.documentation = new vscode.MarkdownString('List of properties of the current Resource. Backed by _org.apache.sling.api.resource.ValueMap_');
				generalCompletions.push(props);
	
				// TODO - deep auto-completion for resource and request
				let req = new vscode.CompletionItem('request');
				req.documentation = new vscode.MarkdownString('The current request. Backed by _org.apache.sling.api.SlingHttpServletRequest_');
				generalCompletions.push(req);

				let htmlDoc = parse(document.getText());
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
	});
}

// this method is called when your extension is deactivated
export function deactivate() {}
