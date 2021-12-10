export interface CompletionData {
    globalCompletions: CompletionDefinition[];
    completionProperties: CompletionProperties[];
}

export interface CompletionDefinition {
    name: string;
    javaType: string;
    description: string | undefined;
}

export interface CompletionProperties {
    javaType: string;
    nestedCompletions: CompletionDefinition[];
}

export class CompletionDataAccess {
    completions: CompletionData;

    constructor(completions: CompletionData) {
        this.completions = completions;
    }

    getGlobalCompletions() {
        return this.completions.globalCompletions;
    }

    findGlobalCompletionDefinition(name: string) {
        return this.completions.globalCompletions.find( element => {
            return element.name === name;
        });
    }

    findPropertyCompletions(javaType: String) {
        let definition = this.completions.completionProperties.find( element => {
            return element.javaType === javaType;
        });
        if ( !definition ) {
            return [];
        }
        return definition.nestedCompletions;
    }
}

export class LocalCompletionDefinition implements CompletionDefinition {
    name: string;
    javaType: string;
    description: string;

    constructor(name: string, javaType: string, description: string) {
        this.javaType = javaType;
        this.name = name;
        this.description = description;
    }
}