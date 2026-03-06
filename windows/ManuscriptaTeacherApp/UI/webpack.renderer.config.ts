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
    fallback: {
      stream: require.resolve('stream-browserify'),
    },
  },
  // mammoth is loaded via script tag and available as window.mammoth
  externals: {
    mammoth: 'mammoth',
  },
};
