/**
 * Default base configuration values per ConfigurationManagementSpecification Appendix 1.
 * Per §1(4): The Windows application shall initialise the base configuration to these values.
 */

import type { ConfigurationEntity } from '../models';

/**
 * Default base configuration matching ConfigurationManagementSpecification Appendix 1.
 * Per Appendix 2, TtsEnabled, AiScaffoldingEnabled, and SummarisationEnabled
 * are not visible or configurable from the frontend.
 */
export const DEFAULT_BASE_CONFIGURATION: ConfigurationEntity = {
    id: '',
    textSize: 6,
    feedbackStyle: 'NEUTRAL',
    ttsEnabled: false,
    aiScaffoldingEnabled: false,
    summarisationEnabled: false,
    mascotSelection: 'NONE',
};
