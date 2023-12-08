type DEL_MED_BRUKER_EVENT = {
  name: "arbeidsmarkedstiltak.del-med-bruker";
  data: {
    action: string;
    tiltakstype: string;
  };
};

export type Event = DEL_MED_BRUKER_EVENT;
