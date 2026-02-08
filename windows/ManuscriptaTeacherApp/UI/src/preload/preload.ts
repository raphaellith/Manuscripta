/**
 * Preload script for Electron.
 * Exposes IPC APIs to renderer via contextBridge.
 * Per FrontendWorkflowSpecifications §4C(4).
 */

import { contextBridge, ipcRenderer } from 'electron';

// Expose electronAPI to the renderer process
contextBridge.exposeInMainWorld('electronAPI', {
    /**
     * Show file picker dialog.
     */
    showOpenDialog: (options: Electron.OpenDialogOptions) =>
        ipcRenderer.invoke('show-open-dialog', options),

    /**
     * Save attachment file to AppData directory.
     * @param sourcePath - Path to the source file
     * @param uuid - UUID for the attachment
     * @param extension - File extension (png, jpeg, pdf)
     * @returns Destination path
     */
    saveAttachmentFile: (sourcePath: string, uuid: string, extension: string) =>
        ipcRenderer.invoke('save-attachment-file', sourcePath, uuid, extension),

    /**
     * Get attachment file path.
     */
    getAttachmentPath: (uuid: string, extension: string) =>
        ipcRenderer.invoke('get-attachment-path', uuid, extension),

    /**
     * Get attachment as data URL for WYSIWYG display.
     */
    getAttachmentDataUrl: (uuid: string, extension: string) =>
        ipcRenderer.invoke('get-attachment-data-url', uuid, extension),

    /**
     * Delete attachment file.
     */
    deleteAttachmentFile: (uuid: string, extension: string) =>
        ipcRenderer.invoke('delete-attachment-file', uuid, extension),

    /**
     * Save attachment file from base64 data (for clipboard paste per §4C(4)(d)(ii)).
     * @param base64Data - Base64-encoded file data
     * @param uuid - UUID for the attachment
     * @param extension - File extension (png, jpeg, pdf)
     * @returns Destination path
     */
    saveAttachmentFromBase64: (base64Data: string, uuid: string, extension: string) =>
        ipcRenderer.invoke('save-attachment-from-base64', base64Data, uuid, extension),

    /**
     * Listen for backend state changes from main process.
     * Per FrontendWorkflowSpecifications §2ZA(6)(c)(i).
     * @param callback - Function to call when backend state changes
     * @returns Function to remove the listener
     */
    onBackendStateChange: (callback: (state: 'reconnecting' | 'connected') => void) => {
        const listener = (_event: Electron.IpcRendererEvent, state: 'reconnecting' | 'connected') => {
            callback(state);
        };
        ipcRenderer.on('backend-state-change', listener);
        return () => {
            ipcRenderer.removeListener('backend-state-change', listener);
        };
    },
});
