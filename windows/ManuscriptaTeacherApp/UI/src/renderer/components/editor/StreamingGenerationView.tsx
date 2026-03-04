/**
 * Streaming Generation View - Displays AI generation output in real-time.
 * Per FrontendWorkflowSpecifications §4B(2)(a1).
 * 
 * Renders as a full-screen modal overlay during AI generation, showing:
 * - Thinking section: Chain-of-thought reasoning (collapsible)
 * - Content section: Generated content (progressively displayed)
 * - Animated cursor indicator during generation
 * - Cancel button to abort generation
 */

import React, { useRef, useEffect } from 'react';
import ReactDOM from 'react-dom';
import './StreamingGenerationView.css';

interface StreamingGenerationViewProps {
    /** Controls visibility of the streaming view. */
    isVisible: boolean;
    /** Accumulated chain-of-thought text. Per §4B(2)(a1)(i). */
    thinkingTokens: string;
    /** Accumulated content text. Per §4B(2)(a1)(ii). */
    contentTokens: string;
    /** Whether generation has finished. Per §4B(2)(a1)(iv). */
    isComplete: boolean;
    /** Callback to cancel the generation. Per FrontendWorkflowSpecifications §4B(2)(a1)(v). */
    onCancel?: () => void;
    /** Whether the generation was cancelled. */
    isCancelled?: boolean;
}

/**
 * StreamingGenerationView component.
 * Per FrontendWorkflowSpecifications §4B(2)(a1).
 */
export const StreamingGenerationView: React.FC<StreamingGenerationViewProps> = ({
    isVisible,
    thinkingTokens,
    contentTokens,
    isComplete,
    onCancel,
    isCancelled = false,
}) => {
    const contentRef = useRef<HTMLDivElement>(null);
    const thinkingRef = useRef<HTMLDetailsElement>(null);

    // Per §4B(2)(a1): Auto-scroll to bottom on new tokens
    useEffect(() => {
        if (contentRef.current) {
            contentRef.current.scrollTop = contentRef.current.scrollHeight;
        }
    }, [contentTokens, thinkingTokens]);

    // Per §4B(2)(a1)(i): Auto-collapse thinking section when content starts arriving
    useEffect(() => {
        if (thinkingRef.current && contentTokens.length > 0 && thinkingRef.current.open) {
            thinkingRef.current.open = false;
        }
    }, [contentTokens]);

    if (!isVisible) {
        return null;
    }

    // Render as portal to ensure full-screen overlay
    return ReactDOM.createPortal(
        <div className="streaming-generation-overlay" role="dialog" aria-modal="true" aria-label="AI Generation Progress">
            <div className="streaming-generation-container">
                {/* Header */}
                <div className="streaming-generation-header">
                    <h2 className="streaming-generation-title">
                        {isCancelled ? 'Generation Cancelled' : isComplete ? 'Generation Complete' : 'Generating Content...'}
                    </h2>
                    {!isComplete && !isCancelled && (
                        <div className="streaming-generation-spinner" aria-label="Loading" />
                    )}
                </div>

                {/* Scrollable content area */}
                <div className="streaming-generation-content" ref={contentRef}>
                    {/* Per §4B(2)(a1)(i): Thinking section - collapsible */}
                    {thinkingTokens.length > 0 && (
                        <details
                            className="streaming-thinking-section"
                            ref={thinkingRef}
                            open={contentTokens.length === 0}
                        >
                            <summary className="streaming-thinking-summary">
                                Thinking…
                            </summary>
                            <div className="streaming-thinking-content">
                                {thinkingTokens}
                            </div>
                        </details>
                    )}

                    {/* Per §4B(2)(a1)(ii): Content section */}
                    <div className="streaming-content-section">
                        {contentTokens}
                        {/* Per §4B(2)(a1)(iii): Blinking cursor while generating */}
                        {!isComplete && !isCancelled && <span className="streaming-cursor">▌</span>}
                    </div>
                </div>

                {/* Per §4B(2)(a1)(v): Cancel button - shown during generation */}
                {!isComplete && !isCancelled && onCancel && (
                    <div className="streaming-generation-footer">
                        <button
                            className="streaming-cancel-button"
                            onClick={onCancel}
                            type="button"
                        >
                            Cancel Generation
                        </button>
                    </div>
                )}
            </div>
        </div>,
        document.body
    );
};

export default StreamingGenerationView;
