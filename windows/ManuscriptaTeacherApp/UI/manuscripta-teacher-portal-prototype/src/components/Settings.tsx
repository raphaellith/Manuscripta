
import React, { useState } from 'react';
import { ContentHeader } from './ContentHeader';
import { Card } from './Card';

interface ToggleProps {
  label: string;
  description: string;
  initialValue?: boolean;
}

const SettingToggle: React.FC<ToggleProps> = ({ label, description, initialValue = true }) => {
  const [isActive, setIsActive] = useState(initialValue);

  return (
    <div className="flex justify-between items-center py-6 border-b border-gray-100 last:border-b-0">
      <div>
        <strong className="text-lg font-sans font-medium text-text-heading">{label}</strong>
        <p className="text-sm text-gray-500 mt-1 font-sans">{description}</p>
      </div>
      <button
        onClick={() => setIsActive(!isActive)}
        className={`w-14 h-8 rounded-full p-1 transition-colors duration-300 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-brand-orange ${isActive ? 'bg-brand-orange' : 'bg-gray-300'}`}
      >
        <div className={`w-6 h-6 rounded-full bg-white shadow-sm transform transition-transform duration-300 ${isActive ? 'translate-x-6' : 'translate-x-0'}`} />
      </button>
    </div>
  );
};


export const Settings: React.FC = () => {
  return (
    <div>
      <div className="space-y-8">
        <Card>
          <h4 className="text-xl font-sans font-semibold text-text-heading mb-4 pb-2 border-b border-gray-100">Accessibility</h4>
          <SettingToggle
            label="Audio Narration"
            description="Enable text-to-speech for all lesson content."
            initialValue={true}
          />
          <SettingToggle
            label="High Contrast Mode"
            description="Increase text contrast on E-Ink displays."
            initialValue={true}
          />
           <SettingToggle
            label="Large Touch Targets"
            description="Increase button size for motor difficulties."
            initialValue={true}
          />
        </Card>

        <Card>
          <h4 className="text-xl font-sans font-semibold text-text-heading mb-4 pb-2 border-b border-gray-100">AI Differentiation</h4>
          <SettingToggle
            label="Auto-Generate Previews"
            description="Automatically generate a preview when base content changes."
            initialValue={true}
          />
          <SettingToggle
            label="Real-Time Suggestions"
            description="Get AI recommendations during lesson creation."
            initialValue={true}
          />
        </Card>

         <Card>
          <h4 className="text-xl font-sans font-semibold text-text-heading mb-4 pb-2 border-b border-gray-100">Notifications</h4>
          <SettingToggle
            label="Help Request Sound"
            description="Play a sound when a student raises their hand for help."
            initialValue={true}
          />
          <SettingToggle
            label="Help Request Toast"
            description="Show a pop-up notification when a student requests help."
            initialValue={true}
          />
          <SettingToggle
            label="Class Completion Alerts"
            description="Notify when the whole class completes a lesson."
            initialValue={true}
          />
          <SettingToggle
            label="Device Status Alerts"
            description="Alert when devices disconnect or have low battery."
            initialValue={false}
          />
        </Card>
        
        <div className="flex justify-end pt-4">
            <button onClick={() => alert('Settings saved!')} className="px-8 py-3 bg-brand-orange text-white font-sans font-medium rounded-md hover:bg-brand-orange-dark transition-colors shadow-sm">
                Save Changes
            </button>
        </div>
      </div>
    </div>
  );
};
