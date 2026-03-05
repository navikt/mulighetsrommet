export type TilskuddFormData = {
  tilskuddstype: string;
  belop: string;
  belopTilUtbetaling?: string;
  nodvendigForOpplaring?: boolean;
  begrunnelse?: string;
  vedtaksresultat?: "innvilgelse" | "avslag";
};

export type BehandlingFormData = {
  journalpostId: string;
  soknadstidspunkt?: Date;
  tilskudd: TilskuddFormData[];
  belopInnenforMaksgrense?: boolean;
  unntakVurdert?: boolean;
  maksbelopBegrunnelse?: string;
  mottakerAvUtbetaling?: "deltaker" | "arrangor";
  kommentarerTilDeltaker?: string;
};
