import type {
  SanityReference,
  SanityKeyedReference,
  SanityAsset,
  SanityImage,
  SanityFile,
  SanityGeoPoint,
  SanityBlock,
  SanityDocument,
  SanityImageCrop,
  SanityImageHotspot,
  SanityKeyed,
  SanityImageAsset,
  SanityImageMetadata,
  SanityImageDimensions,
  SanityImagePalette,
  SanityImagePaletteSwatch,
} from 'sanity-codegen';

export type {
  SanityReference,
  SanityKeyedReference,
  SanityAsset,
  SanityImage,
  SanityFile,
  SanityGeoPoint,
  SanityBlock,
  SanityDocument,
  SanityImageCrop,
  SanityImageHotspot,
  SanityKeyed,
  SanityImageAsset,
  SanityImageMetadata,
  SanityImageDimensions,
  SanityImagePalette,
  SanityImagePaletteSwatch,
};

/**
 * Tiltakstype
 *
 *
 */
export interface SanityTiltakstype extends SanityDocument {
  _type: 'tiltakstype';

  /**
   * Navn på tiltakstype — `string`
   *
   *
   */
  tiltakstypeNavn?: string;

  /**
   * Beskrivelse — `blockContent`
   *
   *
   */
  beskrivelse?: SanityBlockContent;

  /**
   * Overgang til arbeid — `blockContent`
   *
   * Hentes fra Arena, usikker på hvordan denne skal vises her
   */
  overgangTilArbeid?: SanityBlockContent;

  /**
   * Innsatsgruppe — `string`
   *
   *
   */
  innsatsgruppe?: 'staninn' | 'sitinn' | 'speinn' | 'varinn';

  /**
   * Varighet — `string`
   *
   *
   */
  varighet?: string;

  /**
   * Regelverk fil — `file`
   *
   *
   */
  regelverkFil?: { _type: 'file'; asset: SanityReference<any> };

  /**
   * Regelverk lenke — `url`
   *
   *
   */
  regelverkLenke?: string;

  /**
   * Innhold faner — `object`
   *
   *
   */
  faneinnhold?: {
    _type: 'faneinnhold';
    /**
     * For hvem - infoboks — `string`
     *
     * Hvis denne har innhold, vises det i en infoboks i fanen 'For hvem'
     */
    forHvemInfoboks?: string;

    /**
     * For hvem — `blockContent`
     *
     *
     */
    forHvem?: SanityBlockContent;

    /**
     * Detaljer og innhold - infoboks — `string`
     *
     * Hvis denne har innhold, vises det i en infoboks i fanen 'Detaljer og innhold'
     */
    detaljerOgInnholdInfoboks?: string;

    /**
     * Detaljer og innhold — `blockContent`
     *
     *
     */
    detaljerOgInnhold?: SanityBlockContent;

    /**
     * Påmelding og varighet - infoboks — `string`
     *
     * Hvis denne har innhold, vises det i en infoboks i fanen 'Påmelding og varighet'
     */
    pameldingOgVarighetInfoboks?: string;

    /**
     * Påmelding og varighet — `blockContent`
     *
     *
     */
    pameldingOgVarighet?: SanityBlockContent;

    /**
     * Innsikt — `blockContent`
     *
     * Hentes fra Arena, usikker på hvordan denne skal vises her
     */
    innsikt?: SanityBlockContent;
  };
}

/**
 * Tiltaksgjennomføring
 *
 *
 */
export interface SanityTiltaksgjennomforing extends SanityDocument {
  _type: 'tiltaksgjennomforing';

  /**
   * Tiltakstype — `reference`
   *
   *
   */
  tiltakstype?: SanityReference<SanityTiltakstype>;

  /**
   * Navn på tiltaksgjennomføring — `string`
   *
   *
   */
  tiltaksgjennomforingNavn?: string;

  /**
   * Beskrivelse — `string`
   *
   *
   */
  beskrivelse?: string;

  /**
   * Tiltaksnummer — `number`
   *
   *
   */
  tiltaksnummer?: number;

  /**
   * Arrangør — `reference`
   *
   *
   */
  kontaktinfoArrangor?: SanityReference<SanityArrangor>;

  /**
   * Lokasjon — `string`
   *
   *
   */
  lokasjon?: string;

  /**
   * Enheter — `object`
   *
   * Hvilke enheter skal ha tilgang til denne tiltaksgjennomføringen?
   */
  enheter?: {
    _type: 'enheter';
    /**
     * Fylke — `string`
     *
     *
     */
    fylke?: 'innlandet' | 'trondelag' | 'vestViken' | 'ostViken';

    /**
     * Ringsaker — `boolean`
     *
     *
     */
    ringsaker?: boolean;

    /**
     * Trondheim — `boolean`
     *
     *
     */
    trondheim?: boolean;

    /**
     * Steinkjer — `boolean`
     *
     *
     */
    steinkjer?: boolean;

    /**
     * Asker — `boolean`
     *
     *
     */
    asker?: boolean;

    /**
     * Lillestrøm — `boolean`
     *
     *
     */
    lillestrom?: boolean;

    /**
     * Sarpsborg — `boolean`
     *
     *
     */
    sarpsborg?: boolean;

    /**
     * Fredrikstad — `boolean`
     *
     *
     */
    fredrikstad?: boolean;

    /**
     * Indre Østfold — `boolean`
     *
     *
     */
    indreOstfold?: boolean;

    /**
     * Skiptvedt/Marker — `boolean`
     *
     *
     */
    skiptvedtMarker?: boolean;
  };

  /**
   * Oppstart — `string`
   *
   *
   */
  oppstart?: 'dato' | 'lopende';

  /**
   * Oppstart dato — `date`
   *
   *
   */
  oppstartsdato?: string;

  /**
   * Innhold faner — `object`
   *
   *
   */
  faneinnhold?: {
    _type: 'faneinnhold';
    /**
     * For hvem - infoboks — `string`
     *
     * Hvis denne har innhold, vises det i en infoboks i fanen 'For hvem'
     */
    forHvemInfoboks?: string;

    /**
     * For hvem — `blockContent`
     *
     *
     */
    forHvem?: SanityBlockContent;

    /**
     * Detaljer og innhold - infoboks — `string`
     *
     * Hvis denne har innhold, vises det i en infoboks i fanen 'Detaljer og innhold'
     */
    detaljerOgInnholdInfoboks?: string;

    /**
     * Detaljer og innhold — `blockContent`
     *
     *
     */
    detaljerOgInnhold?: SanityBlockContent;

    /**
     * Påmelding og varighet - infoboks — `string`
     *
     * Hvis denne har innhold, vises det i en infoboks i fanen 'Påmelding og varighet'
     */
    pameldingOgVarighetInfoboks?: string;

    /**
     * Påmelding og varighet — `blockContent`
     *
     *
     */
    pameldingOgVarighet?: SanityBlockContent;
  };

  /**
   * Tiltaksansvarlig — `reference`
   *
   *
   */
  kontaktinfoTiltaksansvarlig?: SanityReference<SanityNavKontaktperson>;
}

/**
 * Arrangør
 *
 *
 */
export interface SanityArrangor extends SanityDocument {
  _type: 'arrangor';

  /**
   * Navn på selskap — `string`
   *
   *
   */
  selskapsnavn?: string;

  /**
   * Telefonnummer — `string`
   *
   *
   */
  telefonnummer?: string;

  /**
   * E-post — `string`
   *
   *
   */
  epost?: string;

  /**
   * Adresse — `string`
   *
   *
   */
  adresse?: string;
}

/**
 * NAV kontaktperson
 *
 *
 */
export interface SanityNavKontaktperson extends SanityDocument {
  _type: 'navKontaktperson';

  /**
   * Navn — `string`
   *
   *
   */
  navn?: string;

  /**
   * NAV-enhet — `string`
   *
   *
   */
  enhet?: string;

  /**
   * Telefonnummer — `string`
   *
   *
   */
  telefonnummer?: string;

  /**
   * E-post — `string`
   *
   *
   */
  epost?: string;
}

export type SanityBlockContent = Array<
  | SanityKeyed<SanityBlock>
  | SanityKeyed<{
      _type: 'image';
      asset: SanityReference<SanityImageAsset>;
      crop?: SanityImageCrop;
      hotspot?: SanityImageHotspot;
    }>
>;

export type Documents = SanityTiltakstype | SanityTiltaksgjennomforing | SanityArrangor | SanityNavKontaktperson;
