interface IElectronAPI {
    showOpenDialog: (options: any) => Promise<any>;
    saveAttachmentFile: (sourcePath: string, uuid: string, extension: string) => Promise<string>;
    getAttachmentPath: (uuid: string, extension: string) => Promise<string>;
    getAttachmentDataUrl: (uuid: string, extension: string) => Promise<string>;
    deleteAttachmentFile: (uuid: string, extension: string) => Promise<void>;
    saveAttachmentFromBase64: (base64Data: string, uuid: string, extension: string) => Promise<string>;
    savePdfFile: (pdfBytes: Uint8Array, defaultFilename: string) => Promise<boolean>;
}

interface Window {
    electronAPI: IElectronAPI;
}
