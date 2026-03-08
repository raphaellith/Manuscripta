import type { Configuration } from 'webpack';

import { rules } from './webpack.rules';
import { plugins } from './webpack.plugins';

rules.push({
  test: /\.css$/,
  use: [{ loader: 'style-loader' }, { loader: 'css-loader' }],
});

export const rendererConfig: Configuration = {
  module: {
    rules,
  },
  plugins,
  resolve: {
    extensions: ['.js', '.ts', '.jsx', '.tsx', '.css'],
    // Prioritize browser entry points
    mainFields: ['browser', 'module', 'main'],
    aliasFields: ['browser'],
    alias: {
      // Use mammoth's self-contained browser bundle to avoid Node.js-only
      // imports (fs, path) that fail in the renderer process.
      mammoth: require.resolve('mammoth/mammoth.browser.min.js'),
    },
    fallback: {
      stream: require.resolve('stream-browserify'),
    },
  },
};
