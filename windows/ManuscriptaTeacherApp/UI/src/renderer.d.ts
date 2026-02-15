interface IElectronAPI {
    /** Get the active backend port. Per §2ZA(8)(c)(iv). */
    getBackendPort: () => Promise<number>;
    showOpenDialog: (options: any) => Promise<any>;
    saveAttachmentFile: (sourcePath: string, uuid: string, extension: string) => Promise<string>;
    getAttachmentPath: (uuid: string, extension: string) => Promise<string>;
    getAttachmentDataUrl: (uuid: string, extension: string) => Promise<string>;
    deleteAttachmentFile: (uuid: string, extension: string) => Promise<void>;
    saveAttachmentFromBase64: (base64Data: string, uuid: string, extension: string) => Promise<string>;
    /** Listen for backend state changes from main process. Per §2ZA(6)(c)(i). */
    onBackendStateChange: (callback: (state: 'reconnecting' | 'connected') => void) => () => void;
}

interface Window {
    electronAPI: IElectronAPI;
}
