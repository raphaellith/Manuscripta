import type { ModuleOptions } from 'webpack';

export const rules: Required<ModuleOptions>['rules'] = [
  // Add support for native node modules
  {
    // We're specifying native_modules in the test because the asset relocator loader generates a
    // "fake" .node file which is really a cjs file.
    test: /native_modules[/\\].+\.node$/,
    use: 'node-loader',
  },
  {
    test: /[/\\]node_modules[/\\].+\.(m?js|node)$/,
    exclude: /pdf\.worker/,
    parser: { amd: false },
    use: {
      loader: '@vercel/webpack-asset-relocator-loader',
      options: {
        outputAssetBase: 'native_modules',
      },
    },
  },
  // Emit the pdfjs worker as a static asset so it can be loaded locally
  // instead of from a CDN (offline reliability & supply-chain safety).
  {
    test: /pdf\.worker\.min\.mjs$/,
    type: 'asset/resource',
  },
  {
    test: /\.tsx?$/,
    exclude: /(node_modules|\.webpack)/,
    use: {
      loader: 'ts-loader',
      options: {
        transpileOnly: true,
      },
    },
  },
  // Handle image assets
  {
    test: /\.(png|jpe?g|gif|svg|webp)$/i,
    type: 'asset/resource',
  },
  // Handle PDF files
  {
    test: /\.pdf$/i,
    type: 'asset/resource',
  },
];
