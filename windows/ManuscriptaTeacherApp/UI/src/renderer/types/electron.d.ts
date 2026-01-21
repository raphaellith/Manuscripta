/**
 * Type declarations for Electron IPC API exposed via preload.
 */

export interface ElectronAPI {
    showOpenDialog: (options: Electron.OpenDialogOptions) => Promise<Electron.OpenDialogReturnValue>;
    saveAttachmentFile: (sourcePath: string, uuid: string, extension: string) => Promise<string>;
    getAttachmentPath: (uuid: string, extension: string) => Promise<string>;
    getAttachmentDataUrl: (uuid: string, extension: string) => Promise<string | null>;
    deleteAttachmentFile: (uuid: string, extension: string) => Promise<boolean>;
}

declare global {
    interface Window {
        electronAPI: ElectronAPI;
    }
}

export { };
