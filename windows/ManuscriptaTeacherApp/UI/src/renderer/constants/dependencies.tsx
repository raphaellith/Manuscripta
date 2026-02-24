import React from 'react';

export interface RuntimeDependencyMetadata {
    name: string;
    description: string;
    manualUrl: string;
    manualPathInfo: React.ReactNode;
}

export const DEPENDENCY_METADATA: Record<string, RuntimeDependencyMetadata> = {
    'rmapi': {
        name: 'rmapi',
        description: 'Required to communicate with reMarkable devices through the reMarkable cloud.',
        manualUrl: 'https://github.com/ddvk/rmapi/releases',
        manualPathInfo: (
            <>
                Download <span className="font-mono">rmapi</span> from GitHub, rename the binary to <span className="font-mono">rmapi.exe</span>, and place it at{' '}
                <span className="font-mono">%AppData%\ManuscriptaTeacherApp\bin\rmapi.exe</span>
            </>
        )
    }
};
