import imageUrlBuilder from '@sanity/image-url';
import sanityClient from '@sanity/client';

const config = {
  projectId: 'xegcworx',
  dataset: 'production',
  apiVersion: '2022-05-13',
  useCdn: process.env.NODE_ENV === 'production',
};

const client = sanityClient(config);

// @ts-ignore
export const urlFor = (source: any) => imageUrlBuilder(config).image(source);

export { client };
