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
});
