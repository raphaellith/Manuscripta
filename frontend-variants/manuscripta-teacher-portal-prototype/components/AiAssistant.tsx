
import React, { useState, useRef, useEffect } from 'react';
import type { Message } from '../types';
import { ContentHeader } from './ContentHeader';
import { Card } from './Card';

const initialMessages: Message[] = [
    { id: 1, sender: 'ai', text: "Hi Jon! I'm your AI teaching assistant. I can help with lesson planning, differentiation strategies, and generating content. How can I assist you today?" }
];

export const AiAssistant: React.FC = () => {
    const [messages, setMessages] = useState<Message[]>(initialMessages);
    const [input, setInput] = useState('');
    const messagesEndRef = useRef<null | HTMLDivElement>(null);

    const scrollToBottom = () => {
        // Use nearest block to avoid scrolling the parents excessively
        messagesEndRef.current?.scrollIntoView({ behavior: "smooth", block: "nearest" });
    }

    useEffect(scrollToBottom, [messages]);

    const handleSend = () => {
        if (!input.trim()) return;

        const newUserMessage: Message = {
            id: Date.now(),
            text: input,
            sender: 'user',
        };

        setMessages(prev => [...prev, newUserMessage]);
        setInput('');

        setTimeout(() => {
            const aiResponseMessage: Message = {
                id: Date.now() + 1,
                sender: 'ai',
                text: "That's a great question. For differentiating 'The Battle of Hastings', I'd suggest focusing on key vocabulary for the lower age cohort and complex causal relationships for the higher one. Would you like me to generate some sample questions for the 'Intermediate (10-13)' level?"
            };
            setMessages(prev => [...prev, aiResponseMessage]);
        }, 1200);
    };

    // Adjusted height to ensure it fits within the main container without triggering parent scroll
    return (
        <div className='flex flex-col' style={{ height: 'calc(100vh - 200px)' }}>
            <Card className="flex-grow flex flex-col p-0 overflow-hidden border-0 shadow-soft min-h-0">
                <div className="flex-grow overflow-y-auto p-6 bg-brand-gray/30">
                    {messages.map(msg => (
                        <div key={msg.id} className={`flex ${msg.sender === 'user' ? 'justify-end' : 'justify-start'} mb-6`}>
                            <div className={`max-w-lg p-5 rounded-lg font-sans leading-relaxed shadow-sm
                                ${msg.sender === 'user' 
                                    ? 'bg-brand-orange text-white rounded-br-none' 
                                    : 'bg-white text-text-body border border-gray-100 rounded-bl-none'
                                }`}>
                                {msg.text}
                            </div>
                        </div>
                    ))}
                    <div ref={messagesEndRef} />
                </div>
                <div className="p-4 bg-white border-t border-gray-100 flex gap-4 flex-shrink-0">
                    <div className="relative flex-grow">
                        <input
                            type="text"
                            value={input}
                            onChange={e => setInput(e.target.value)}
                            onKeyPress={e => e.key === 'Enter' && handleSend()}
                            placeholder="Ask about teaching or differentiation..."
                            className="w-full p-4 bg-brand-gray text-text-body font-sans rounded-lg border-2 border-transparent focus:border-brand-orange focus:outline-none"
                        />
                    </div>
                    <button onClick={handleSend} className="px-8 py-3 bg-brand-green text-white font-sans font-medium rounded-md hover:bg-green-900 transition-colors shadow-sm">
                        Send
                    </button>
                </div>
            </Card>
        </div>
    );
};
