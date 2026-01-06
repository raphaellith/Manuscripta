
import React from 'react';
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, Cell } from 'recharts';
import { ContentHeader } from './ContentHeader';
import { Card } from './Card';

const pollData = [
  { name: 'Option A', votes: 12, color: '#FF6106' }, // Orange
  { name: 'Option B', votes: 8, color: '#15502E' },  // Green
  { name: 'Option C', votes: 5, color: '#ABD8F9' },  // Blue
  { name: 'Option D', votes: 3, color: '#FFE782' },  // Yellow
];

export const ClassDashboard: React.FC = () => {
  return (
    <div>
      <div className="grid grid-cols-1 md:grid-cols-3 gap-8 mb-8">
        <Card className="border-t-4 border-t-brand-green">
          <h4 className="font-sans font-semibold text-xl text-text-heading mb-4">Device Status</h4>
          <div className="flex items-end gap-3 mb-2">
            <div className="text-5xl font-sans font-medium text-brand-green">28</div>
            <div className="text-2xl text-gray-400 font-sans mb-1">/30</div>
          </div>
           <div>
                <p className="text-sm font-medium text-text-heading">Devices Connected</p>
                <p className="text-sm text-brand-green font-medium mt-1 flex items-center gap-1">
                    <span className="w-2 h-2 rounded-full bg-brand-green inline-block"></span>
                    Ready for lesson
                </p>
            </div>
        </Card>
        <Card className="border-t-4 border-t-brand-blue">
          <h4 className="font-sans font-semibold text-xl text-text-heading mb-4">Lesson Progress</h4>
           <div className="flex items-end gap-3 mb-2">
            <div className="text-5xl font-sans font-medium text-blue-400">65</div>
            <div className="text-2xl text-gray-400 font-sans mb-1">%</div>
           </div>
            <div>
                <p className="text-sm font-medium text-text-heading">Average Completion</p>
                <div className="w-full bg-gray-100 rounded-full h-2 mt-2">
                    <div className="bg-brand-blue h-2 rounded-full" style={{ width: '65%' }}></div>
                </div>
            </div>
        </Card>
        <Card className="border-t-4 border-t-brand-orange">
          <h4 className="font-sans font-semibold text-xl text-text-heading mb-4">Alerts</h4>
          <div className="flex items-end gap-3 mb-2">
            <div className="text-5xl font-sans font-medium text-brand-orange">2</div>
          </div>
            <div>
                <p className="text-sm font-medium text-text-heading">Devices need attention</p>
                <p className="text-sm text-gray-500 mt-1">e.g., low battery, disconnected</p>
            </div>
        </Card>
      </div>

      <div className="grid grid-cols-1 gap-8">
        <Card>
          <h4 className="font-sans font-semibold text-xl text-text-heading mb-6">Live Poll Results: "Who led the Norman army?"</h4>
          <div style={{ width: '100%', height: 300 }}>
            <ResponsiveContainer>
              <BarChart data={pollData} margin={{ top: 5, right: 20, left: -10, bottom: 5 }}>
                <CartesianGrid strokeDasharray="3 3" stroke="#e5e7eb" vertical={false} />
                <XAxis dataKey="name" axisLine={false} tickLine={false} tick={{fill: '#6b7280', fontSize: 12}} dy={10} />
                <YAxis axisLine={false} tickLine={false} tick={{fill: '#6b7280', fontSize: 12}} />
                <Tooltip 
                    contentStyle={{ borderRadius: '6px', border: 'none', boxShadow: '0 4px 20px rgba(0,0,0,0.08)' }}
                    cursor={{fill: '#F8F7F5'}}
                />
                <Bar dataKey="votes" radius={[4, 4, 0, 0]} barSize={50}>
                    {pollData.map((entry, index) => (
                        <Cell key={`cell-${index}`} fill={entry.color} />
                    ))}
                </Bar>
              </BarChart>
            </ResponsiveContainer>
          </div>
          <div className="flex justify-center gap-6 mt-6">
              <div className="flex items-center gap-2">
                  <span className="w-3 h-3 rounded-full bg-brand-orange"></span>
                  <span className="text-sm text-gray-600">Option A</span>
              </div>
              <div className="flex items-center gap-2">
                  <span className="w-3 h-3 rounded-full bg-brand-green"></span>
                  <span className="text-sm text-gray-600">Option B</span>
              </div>
               <div className="flex items-center gap-2">
                  <span className="w-3 h-3 rounded-full bg-brand-blue"></span>
                  <span className="text-sm text-gray-600">Option C</span>
              </div>
               <div className="flex items-center gap-2">
                  <span className="w-3 h-3 rounded-full bg-brand-yellow"></span>
                  <span className="text-sm text-gray-600">Option D</span>
              </div>
          </div>
        </Card>
      </div>
    </div>
  );
};
