import { ClientConfig, createClient } from 'next-sanity';
import imageUrlBuilder from '@sanity/image-url';

const config: ClientConfig = {
  projectId: 'xegcworx',
  dataset: 'production',
  apiVersion: '2022-05-13',
  useCdn: process.env.NODE_ENV === 'production',
};

// @ts-ignore
export const urlFor = (source: any) => imageUrlBuilder(config).image(source);

export const sanityClient = createClient(config);
