import React from 'react';
import { Innsatsgruppe } from '../../../mulighetsrommet-api-client';
import { GetStaticPaths, GetStaticProps } from 'next';
import groq from 'groq';
import { sanityClient } from '../sanityClient';

const pathQuery = groq`*[_type == "tiltakstype"`;

export const getStaticPaths: GetStaticPaths = async context => {
  const tiltakstypeSlugs = await sanityClient.fetch(pathQuery);
  return {
    paths: tiltakstypeSlugs.filter(Boolean).map((slug: string) => ({ params: { slug } })),
    fallback: true,
  };
};

export const groqTiltakstypePreviewFields = `
tittel,
slug,
_createdAt,
mainImage,
_id,
language,
forfattere[]-> {
  navn,
  mainImage,
  _id
}
`;

export interface TiltakstypeI {
  _id: string;
  title: string;
  ingress: string;
  innsatsgruppe: Innsatsgruppe;
  oppstart: string;
  faneinnhold: {
    detaljeroginnhold: string;
    forhvem: string;
    pameldingogvarighet: string;
    kontaktinfofagansvarlig: { fagansvarlig: string; telefonnummer: number; epost: string; adresse: string };
  };
}

const tiltakstypeQuery = groq`
  *[_type == "tiltakstype" && slug.current == $slug][0] {
    title,
    _createdAt,
    _id,
    ingress,
    innsatsgruppe,
    oppstart,
    "slug": slug.current,
    language,
    faneinnhold[]-> {
      detaljeroginnhold,
      forhvem,
      pameldingogvarighet,
      kontaktinfofagansvarlig[]-> {
      fagansvarlig,
      telefonnummer,
      epost,
      adresse
      }
    }
  }
`;

export const getStaticProps: GetStaticProps = async ctx => {
  const slug = ctx.params?.slug;
  const data = await sanityClient.fetch(tiltakstypeQuery, { slug });
  return {
    props: { data, slug },
    revalidate: 60,
  };
};
//
// const PreviewWrapper = (props: { data: TiltakstypeI; slug?: string }) => {
//   const { data } = usePreviewSubscription(tiltakstypeQuery, {
//     params: { slug: props?.slug },
//     initialData: props.data,
//   });
//
//   return (
//     <>
//       <ViewSanity {...data} />
//     </>
//   );
// };

// export default PreviewWrapper;
