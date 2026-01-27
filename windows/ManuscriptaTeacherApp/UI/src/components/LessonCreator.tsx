
import React, { useState } from 'react';
import { ContentHeader } from './ContentHeader';
import { Card } from '../renderer/components/common/Card';

const ageGroupLevels = [
    { value: 0, label: "4-6" },
    { value: 1, label: "7-9" },
    { value: 2, label: "10-13" },
    { value: 3, label: "14+" },
];

const UploadIcon = () => (
    <svg xmlns="http://www.w3.org/2000/svg" className="h-10 w-10 text-brand-orange/50" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={1}>
        <path strokeLinecap="round" strokeLinejoin="round" d="M7 16a4 4 0 01-.88-7.903A5 5 0 1115.9 6L16 6a5 5 0 011 9.9M15 13l-3-3m0 0l-3 3m3-3v12" />
    </svg>
);


interface LessonCreatorProps {
    onSave: (unitData: { title: string; subject: string; description: string; }) => void;
    onBack: () => void;
}

export const LessonCreator: React.FC<LessonCreatorProps> = ({ onSave, onBack }) => {
    const [title, setTitle] = useState('');
    const [subject, setSubject] = useState('');
    const [description, setDescription] = useState('');
    const [files, setFiles] = useState<File[]>([]);

    const handleFileChange = (event: React.ChangeEvent<HTMLInputElement>) => {
        if (event.target.files) {
            setFiles(prev => [...prev, ...Array.from(event.target.files as FileList)]);
        }
    };

    const handleSaveUnit = () => {
        if (!title.trim() || !subject.trim() || !description.trim()) {
            alert('Please fill out all fields before saving.');
            return;
        }
        onSave({
            title,
            subject,
            description,
        });
    }
    
  return (
    <div>
      <Card>
        <div className="space-y-8">
            {/* Basic Info */}
            <div className='grid grid-cols-1 md:grid-cols-2 gap-8'>
                <div>
                    <label className="font-sans font-medium text-text-heading text-sm mb-2 block">Unit / Topic Title</label>
                    <input 
                        type="text" 
                        value={title}
                        onChange={(e) => setTitle(e.target.value)}
                        className="w-full p-3 bg-white text-text-body font-sans rounded-lg border border-gray-200 focus:border-brand-orange focus:ring-1 focus:ring-brand-orange focus:outline-none transition-all" 
                        placeholder="e.g., The Norman Conquest" 
                    />
                </div>
                <div>
                    <label className="font-sans font-medium text-text-heading text-sm mb-2 block">Subject</label>
                    <input 
                        type="text" 
                        value={subject}
                        onChange={(e) => setSubject(e.target.value)}
                        className="w-full p-3 bg-white text-text-body font-sans rounded-lg border border-gray-200 focus:border-brand-orange focus:ring-1 focus:ring-brand-orange focus:outline-none transition-all" 
                        placeholder="e.g., History" 
                    />
                </div>
            </div>
          
            <div>
                <label className="font-sans font-medium text-text-heading text-sm mb-2 block">Description / Learning Objectives</label>
                <textarea 
                    value={description}
                    onChange={(e) => setDescription(e.target.value)}
                    className="w-full p-3 bg-white text-text-body font-sans rounded-lg border border-gray-200 focus:border-brand-orange focus:ring-1 focus:ring-brand-orange focus:outline-none transition-all resize-none" 
                    placeholder="e.g., Students will learn about the causes, events, and consequences of the Norman invasion of England."
                    rows={4}
                />
            </div>

            {/* File Upload */}
            <div>
                <label className="font-sans font-medium text-text-heading text-sm mb-2 block">Source Materials</label>
                <div className="border-2 border-dashed border-gray-200 rounded-lg p-10 text-center hover:border-brand-orange/50 transition-colors bg-brand-gray/30">
                    <div className='flex justify-center mb-4'>
                        <div className="p-4 bg-brand-orange-light rounded-full">
                           <UploadIcon />
                        </div>
                    </div>
                    <p className="font-medium text-text-heading">Drag & drop files here or</p>
                    <label htmlFor="file-upload" className="mt-1 inline-block cursor-pointer text-brand-orange font-semibold hover:text-brand-orange-dark underline decoration-2 underline-offset-2">
                        browse from computer
                        <input id="file-upload" name="file-upload" type="file" className="sr-only" multiple onChange={handleFileChange} />
                    </label>
                    <p className="text-sm text-gray-500 mt-2">PDF, TXT, DOCX up to 10MB</p>
                </div>
                {files.length > 0 && (
                    <div className='mt-6 bg-white border border-gray-100 rounded-lg p-4 shadow-sm'>
                        <h4 className='font-sans font-medium text-text-heading mb-2'>Uploaded files:</h4>
                        <ul className='space-y-2'>
                            {files.map((file, index) => (
                                <li key={index} className="flex items-center text-sm text-gray-600 bg-brand-gray p-2 rounded">
                                    <span className="w-2 h-2 bg-brand-green rounded-full mr-2"></span>
                                    {file.name} <span className="ml-auto text-gray-400">({Math.round(file.size / 1024)} KB)</span>
                                </li>
                            ))}
                        </ul>
                    </div>
                )}
            </div>
           
           <div className="flex flex-wrap gap-4 pt-6 border-t border-gray-100">
               <button onClick={handleSaveUnit} className="px-6 py-3 bg-brand-orange text-white font-sans font-medium rounded-md hover:bg-brand-orange-dark transition-colors shadow-sm">Save Unit</button>
               <button onClick={onBack} className="px-6 py-3 bg-white text-gray-600 border border-gray-300 font-sans font-medium rounded-md hover:border-brand-orange hover:text-brand-orange transition-colors">Cancel</button>
           </div>
        </div>
      </Card>
    </div>
  );
};
