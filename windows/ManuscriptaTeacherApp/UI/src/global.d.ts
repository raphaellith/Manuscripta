/**
 * Global TypeScript declarations for Electron IPC APIs.
 * Per FrontendWorkflowSpecifications §4C(4).
 */

/**
 * Electron API exposed via contextBridge in preload.ts.
 */
interface ElectronAPI {
    /**
     * Get the active backend port.
     * Per FrontendWorkflowSpecifications §2ZA(8)(c)(iv).
     */
    getBackendPort: () => Promise<number>;

    /**
     * Show file picker dialog.
     * Returns file tokens for secure access via readFileBuffer.
     */
    showOpenDialog: (options: Electron.OpenDialogOptions) => Promise<{ canceled: boolean; filePaths: string[]; fileTokens: string[] }>;

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

    /**
     * Read file contents as ArrayBuffer using a secure token from showOpenDialog.
     * Per FrontendWorkflowSpecifications §4AA(2)(b).
     */
    readFileBuffer: (fileToken: string) => Promise<ArrayBuffer>;

    /**
     * Listen for backend state changes from main process.
     * Per FrontendWorkflowSpecifications §2ZA(6)(c)(i).
     * @param callback - Function to call when backend state changes
     * @returns Function to remove the listener
     */
    onBackendStateChange: (callback: (state: 'reconnecting' | 'connected') => void) => () => void;

    /**
     * Show save dialog and save PDF file.
     * Per FrontendWorkflowSpecifications §4D(2)(a).
     * @param pdfBytes - PDF file content as Uint8Array
     * @param defaultFilename - Default filename for save dialog
     * @returns true if saved successfully, false if cancelled
     */
    savePdfFile: (pdfBytes: Uint8Array, defaultFilename: string) => Promise<boolean>;
}

declare global {
    interface Window {
        electronAPI: ElectronAPI;
    }
}

export { };
