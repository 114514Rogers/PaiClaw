import { baseURL } from '@/utils/http';

const workflowAssetModules = import.meta.glob(
  '/src/assets/imgs/workflow/*.{png,svg,jpg,jpeg,webp}',
  {
    eager: true,
    import: 'default',
  }
) as Record<string, string>;

const workflowAssetMap = Object.fromEntries(
  Object.entries(workflowAssetModules).map(([path, assetUrl]) => [
    `/workflow/${path.split('/').pop()}`,
    assetUrl,
  ])
);

export const resolveImageUrl = (url?: string): string => {
  if (!url) {
    return '';
  }

  if (
    url.startsWith('http://') ||
    url.startsWith('https://') ||
    url.startsWith('data:') ||
    url.startsWith('blob:')
  ) {
    return url;
  }

  const normalizedUrl = url.startsWith('/') ? url : `/${url}`;

  if (workflowAssetMap[normalizedUrl]) {
    return workflowAssetMap[normalizedUrl];
  }

  return `${baseURL}${normalizedUrl}`;
};
