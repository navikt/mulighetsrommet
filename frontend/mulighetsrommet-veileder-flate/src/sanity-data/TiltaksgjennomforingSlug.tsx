import React from 'react';
import { TiltakstypeI } from './TiltakstypeSlug';
import groq from 'groq';
import { GetStaticPaths, GetStaticProps } from 'next';
import { sanityClient } from '../sanityClient';

const pathQuery = groq`*[_type == "tiltaksgjennomforing"]`;

export const getStaticPaths: GetStaticPaths = async context => {
  const tiltaksgjennomforingSlugs = await sanityClient.fetch(pathQuery);
  return {
    paths: tiltaksgjennomforingSlugs.filter(Boolean).map((slug: string) => ({ params: { slug } })),
    fallback: true,
  };
};

const tiltaksgjennomforingQuery = groq` 
*[_type == "tiltaksgjennomforing"`;

export interface TiltaksgjennomforingI {
  _id: string;
  title: string;
  tiltakstype: TiltakstypeI;
  tiltaksnummer: number;
  leverandor: string;
  oppstartsdato?: Date;
  faneinnhold: {
    forhvem: string;
    detaljeroginnhold: string;
    pameldingogvarighet: string;
    kontaktinfofagansvarlig: { kontaktinfoleverandor: string; kontaktinfotiltaksansvarlig: string };
  };
}

export const getStaticProps: GetStaticProps = async ctx => {
  const data = await sanityClient.fetch(tiltaksgjennomforingQuery, {});
  return {
    props: { data },
    revalidate: 600,
  };
};

// const PreviewWrapper = (props: { data: TiltaksgjennomforingI }) => {
//   const { data } = usePreviewSubscription(tiltaksgjennomforingQuery, {
//     initialData: props.data,
//   });
//
//   return <TiltakstypeTabell {...data} />;
// };
//
// export default PreviewWrapper;
