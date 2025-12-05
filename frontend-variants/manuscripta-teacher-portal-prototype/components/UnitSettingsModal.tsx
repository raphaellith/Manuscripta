import React, { useState } from 'react';
import type { Unit, SourceMaterial } from '../types';

interface UnitSettingsModalProps {
  unit: Unit;
  onClose: () => void;
  onSave: (updatedUnit: Unit) => void;
}

const UploadIcon = () => (
  <svg xmlns="http://www.w3.org/2000/svg" className="h-8 w-8 text-brand-orange/50" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={1}>
    <path strokeLinecap="round" strokeLinejoin="round" d="M7 16a4 4 0 01-.88-7.903A5 5 0 1115.9 6L16 6a5 5 0 011 9.9M15 13l-3-3m0 0l-3 3m3-3v12" />
  </svg>
);

const getSourceIcon = (type: SourceMaterial['type']) => {
  switch (type) {
    case 'textbook':
      return (
        <svg xmlns="http://www.w3.org/2000/svg" className="h-5 w-5 text-brand-green" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.747 0 3.332.477 4.5 1.253v13C19.832 18.477 18.247 18 16.5 18c-1.746 0-3.332.477-4.5 1.253" />
        </svg>
      );
    case 'pdf':
      return (
        <svg xmlns="http://www.w3.org/2000/svg" className="h-5 w-5 text-red-500" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M7 21h10a2 2 0 002-2V9.414a1 1 0 00-.293-.707l-5.414-5.414A1 1 0 0012.586 3H7a2 2 0 00-2 2v14a2 2 0 002 2z" />
        </svg>
      );
    case 'document':
      return (
        <svg xmlns="http://www.w3.org/2000/svg" className="h-5 w-5 text-brand-blue" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
        </svg>
      );
    case 'notes':
      return (
        <svg xmlns="http://www.w3.org/2000/svg" className="h-5 w-5 text-brand-yellow" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
        </svg>
      );
    default:
      return null;
  }
};

export const UnitSettingsModal: React.FC<UnitSettingsModalProps> = ({ unit, onClose, onSave }) => {
  const [title, setTitle] = useState(unit.title);
  const [subject, setSubject] = useState(unit.subject);
  const [description, setDescription] = useState(unit.description);
  const [sourceMaterials, setSourceMaterials] = useState<SourceMaterial[]>(unit.sourceMaterials || []);

  const handleSave = () => {
    if (!title.trim() || !subject.trim()) {
      alert('Please fill out the title and subject fields.');
      return;
    }
    onSave({
      ...unit,
      title,
      subject,
      description,
      sourceMaterials,
    });
  };

  const handleDeleteMaterial = (id: string) => {
    setSourceMaterials(prev => prev.filter(m => m.id !== id));
  };

  const handleAddMaterial = () => {
    // Simulate adding a new material (in real app, this would trigger file upload)
    const mockMaterial: SourceMaterial = {
      id: `material-${Date.now()}`,
      name: 'New Resource.pdf',
      type: 'pdf',
      size: '1.2 MB',
      addedDate: new Date().toLocaleDateString('en-GB', { day: 'numeric', month: 'short', year: 'numeric' }),
    };
    setSourceMaterials(prev => [...prev, mockMaterial]);
  };

  return (
    <div className="fixed inset-0 bg-text-heading/20 backdrop-blur-sm flex items-center justify-center z-50 p-4">
      <div className="bg-white rounded-xl shadow-2xl w-full max-w-2xl max-h-[90vh] overflow-hidden flex flex-col">
        {/* Header */}
        <div className="px-8 py-6 border-b border-gray-100 flex items-center justify-between bg-gradient-to-r from-brand-cream to-white">
          <div>
            <h2 className="text-2xl font-semibold text-text-heading">Unit Settings</h2>
            <p className="text-sm text-gray-500 mt-1">Configure unit details and source materials</p>
          </div>
          <button
            onClick={onClose}
            className="p-2 hover:bg-gray-100 rounded-lg transition-colors"
          >
            <svg xmlns="http://www.w3.org/2000/svg" className="h-5 w-5 text-gray-500" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>

        {/* Content */}
        <div className="flex-1 overflow-y-auto p-8 space-y-6">
          {/* Basic Info */}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            <div>
              <label className="font-sans font-medium text-text-heading text-sm mb-2 block">Unit Title</label>
              <input
                type="text"
                value={title}
                onChange={(e) => setTitle(e.target.value)}
                className="w-full p-3 bg-brand-gray text-text-body font-sans rounded-lg border-2 border-transparent focus:border-brand-orange focus:outline-none transition-all"
                placeholder="e.g., The Norman Conquest"
              />
            </div>
            <div>
              <label className="font-sans font-medium text-text-heading text-sm mb-2 block">Subject</label>
              <input
                type="text"
                value={subject}
                onChange={(e) => setSubject(e.target.value)}
                className="w-full p-3 bg-brand-gray text-text-body font-sans rounded-lg border-2 border-transparent focus:border-brand-orange focus:outline-none transition-all"
                placeholder="e.g., History"
              />
            </div>
          </div>

          <div>
            <label className="font-sans font-medium text-text-heading text-sm mb-2 block">Description / Learning Objectives</label>
            <textarea
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              className="w-full p-3 bg-brand-gray text-text-body font-sans rounded-lg border-2 border-transparent focus:border-brand-orange focus:outline-none transition-all resize-none"
              placeholder="Describe what students will learn in this unit..."
              rows={3}
            />
          </div>

          {/* Source Materials Section */}
          <div>
            <div className="flex items-center justify-between mb-3">
              <div>
                <label className="font-sans font-medium text-text-heading text-sm block">Source Materials</label>
                <p className="text-xs text-gray-500 mt-0.5">Textbooks and resources used for AI lesson generation</p>
              </div>
              <span className="text-xs text-gray-400 bg-gray-100 px-2 py-1 rounded">
                {sourceMaterials.length} resource{sourceMaterials.length !== 1 ? 's' : ''}
              </span>
            </div>

            {/* Existing Materials */}
            {sourceMaterials.length > 0 && (
              <div className="space-y-2 mb-4">
                {sourceMaterials.map((material) => (
                  <div
                    key={material.id}
                    className="flex items-center gap-3 p-3 bg-white border border-gray-200 rounded-lg group hover:border-gray-300 transition-colors"
                  >
                    <div className="p-2 bg-gray-50 rounded-md">
                      {getSourceIcon(material.type)}
                    </div>
                    <div className="flex-1 min-w-0">
                      <p className="font-medium text-text-heading text-sm truncate">{material.name}</p>
                      <p className="text-xs text-gray-400">
                        {material.type.charAt(0).toUpperCase() + material.type.slice(1)}
                        {material.size && ` - ${material.size}`}
                        {material.pages && ` - ${material.pages} pages`}
                        {' - Added '}{material.addedDate}
                      </p>
                    </div>
                    <button
                      onClick={() => handleDeleteMaterial(material.id)}
                      className="p-2 text-gray-400 hover:text-red-500 hover:bg-red-50 rounded-md opacity-0 group-hover:opacity-100 transition-all"
                      title="Remove material"
                    >
                      <svg xmlns="http://www.w3.org/2000/svg" className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                      </svg>
                    </button>
                  </div>
                ))}
              </div>
            )}

            {/* Upload Area */}
            <div className="border-2 border-dashed border-gray-200 rounded-lg p-6 text-center hover:border-brand-orange/50 transition-colors bg-brand-gray/30">
              <div className="flex justify-center mb-3">
                <div className="p-3 bg-brand-orange-light rounded-full">
                  <UploadIcon />
                </div>
              </div>
              <p className="font-medium text-text-heading text-sm">Drag and drop files here or</p>
              <button
                onClick={handleAddMaterial}
                className="mt-1 text-brand-orange font-semibold hover:text-brand-orange-dark underline decoration-2 underline-offset-2 text-sm"
              >
                browse from computer
              </button>
              <p className="text-xs text-gray-400 mt-2">PDF, TXT, DOCX files up to 10MB</p>
            </div>
          </div>
        </div>

        {/* Footer */}
        <div className="px-8 py-4 border-t border-gray-100 bg-gray-50 flex justify-end gap-3">
          <button
            onClick={onClose}
            className="px-6 py-2.5 bg-white text-gray-600 border border-gray-300 font-sans font-medium rounded-md hover:bg-gray-50 transition-colors"
          >
            Cancel
          </button>
          <button
            onClick={handleSave}
            className="px-6 py-2.5 bg-brand-orange text-white font-sans font-medium rounded-md hover:bg-brand-orange-dark transition-colors shadow-sm"
          >
            Save Changes
          </button>
        </div>
      </div>
    </div>
  );
};
