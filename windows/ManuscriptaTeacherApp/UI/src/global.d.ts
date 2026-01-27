/**
 * Global TypeScript declarations for Electron IPC APIs.
 * Per FrontendWorkflowSpecifications §4C(4).
 */

/**
 * Electron API exposed via contextBridge in preload.ts.
 */
interface ElectronAPI {
    /**
     * Show file picker dialog.
     */
    showOpenDialog: (options: Electron.OpenDialogOptions) => Promise<Electron.OpenDialogReturnValue>;

    /**
     * Save attachment file to AppData directory.
     * @param sourcePath - Path to the source file
     * @param uuid - UUID for the attachment
     * @param extension - File extension (png, jpeg, pdf)
     * @returns Destination path
     */
    saveAttachmentFile: (sourcePath: string, uuid: string, extension: string) => Promise<string>;

    /**
     * Get attachment file path.
     */
    getAttachmentPath: (uuid: string, extension: string) => Promise<string>;

    /**
     * Get attachment as data URL for WYSIWYG display.
     */
    getAttachmentDataUrl: (uuid: string, extension: string) => Promise<string | null>;

    /**
     * Delete attachment file.
     */
    deleteAttachmentFile: (uuid: string, extension: string) => Promise<boolean>;

    /**
     * Save attachment file from base64 data (for clipboard paste per §4C(4)(d)(ii)).
     * @param base64Data - Base64-encoded file data
     * @param uuid - UUID for the attachment
     * @param extension - File extension (png, jpeg)
     * @returns Destination path
     */
    saveAttachmentFromBase64: (base64Data: string, uuid: string, extension: string) => Promise<string>;
}

declare global {
    interface Window {
        electronAPI: ElectronAPI;
    }
}

export { };
