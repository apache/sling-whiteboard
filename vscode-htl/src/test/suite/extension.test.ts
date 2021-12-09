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

	test('completion test for request', () => {
		let document = `
			<html>
				<body>
					\${ request. }
				</body>
			</html>
		`;
		let completions = completionProvider.provideCompletionItems0('${request.', document);
		// test a subset, otherwise it's too cumbersome
		let completionVariables = completions?.map ( c => c.label.toString()).slice(0,5);
		assert.deepStrictEqual(completionVariables?.sort(), ["requestParameterList", "requestParameterMap", "requestPathInfo", "resource", "resourceResolver"]);
	});

	test('completion test for resource', () => {
		let document = `
			<html>
				<body>
					\${ resource. }
				</body>
			</html>
		`;
		let completions = completionProvider.provideCompletionItems0('${resource.', document);
		// test a subset, otherwise it's too cumbersome
		let completionVariables = completions?.map ( c => c.label.toString()).slice(0,5);
		assert.deepStrictEqual(completionVariables?.sort(), ["children", "name", "parent", "path", "resourceType"]);
	});

	test('nested completion test for', () => {
		let document = `
			<html>
				<body>
					\${ request.resource.parent. }
				</body>
			</html>
		`;
		let completions = completionProvider.provideCompletionItems0('${request.resource.parent.', document);
		// test a subset, otherwise it's too cumbersome
		let completionVariables = completions?.map ( c => c.label.toString()).slice(0,5);
		assert.deepStrictEqual(completionVariables?.sort(), ["children", "name", "parent", "path", "resourceType"]);
	});
});
