import * as assert from 'assert';
import { HtlCompletionItemProvider } from '../../htlCompletionItemProvider';

// You can import and use all API from the 'vscode' module
// as well as import your extension to test it
import * as vscode from 'vscode';
// import * as myExtension from '../../extension';

suite('Extension Test Suite',  () => {
	vscode.window.showInformationMessage('Start all tests.');
	const workingDir = vscode.Uri.parse("file://" + __dirname, true);
	const completionsPath = vscode.Uri.joinPath(workingDir, "..", "..", "..", "data");
	const completionProvider = new HtlCompletionItemProvider(completionsPath);
	
	test('completion test with no additional structures', () => {
		let document = `
			<html>
				<body>
					\${ }
				</body>
			</html>
		`;
		let completions = completionProvider.provideCompletionItems0('${', document);
		let completionVariables = completions?.map ( c => c.label.toString());
		assert.deepStrictEqual(completionVariables?.sort(), ["properties", "request", "resolver", "resource", "response"]);
	});

	test('completion test with data-sly-use', () => {
		let document = `
			<html>
				<body data-sly-use.foo="foo.js">
					\${ }
				</body>
			</html>
		`;
		let completions = completionProvider.provideCompletionItems0('${', document);
		let completionVariables = completions?.map ( c => c.label.toString());
		assert.deepStrictEqual(completionVariables?.sort(), ["foo", "properties", "request", "resolver", "resource", "response"]);
	});

	test('completion test with data-sly-repeat', () => {
		let document = `
			<html>
				<body data-sly-repeat="\${pageItems}">
					\${ }
				</body>
			</html>
		`;
		let completions = completionProvider.provideCompletionItems0('${', document);
		let completionVariables = completions?.map ( c => c.label.toString());
		assert.deepStrictEqual(completionVariables?.sort(), ["item", "itemList", "properties", "request", "resolver", "resource", "response"]);
	});
});
