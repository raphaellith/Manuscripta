interface IElectronAPI {
    /** Get the active backend port. Per §2ZA(8)(c)(iv). */
    getBackendPort: () => Promise<number>;
    showOpenDialog: (options: any) => Promise<{ canceled: boolean; filePaths: string[]; fileTokens: string[] }>;
    saveAttachmentFile: (sourcePath: string, uuid: string, extension: string) => Promise<string>;
    getAttachmentPath: (uuid: string, extension: string) => Promise<string>;
    getAttachmentDataUrl: (uuid: string, extension: string) => Promise<string>;
    deleteAttachmentFile: (uuid: string, extension: string) => Promise<void>;
    saveAttachmentFromBase64: (base64Data: string, uuid: string, extension: string) => Promise<string>;
    savePdfFile: (pdfBytes: Uint8Array, defaultFilename: string) => Promise<boolean>;
    /** Read file contents as ArrayBuffer using a secure token from showOpenDialog. Per §4AA(2)(b). */
    readFileBuffer: (fileToken: string) => Promise<ArrayBuffer>;
    /** Listen for backend state changes from main process. Per §2ZA(6)(c)(i). */
    onBackendStateChange: (callback: (state: 'reconnecting' | 'connected') => void) => () => void;
}

interface Window {
    electronAPI: IElectronAPI;
}

declare module '@tiptap/react/menus' {
    import * as React from 'react';

    export const BubbleMenu: React.ComponentType<Record<string, unknown>>;
    export const FloatingMenu: React.ComponentType<Record<string, unknown>>;
}
