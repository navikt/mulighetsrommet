import adminStructure from "./adminStructure";
import redaktorAvdirStructure from "./redaktorAvdirStructure";
import redaktorTiltaksgjennomforingStructure from "./redaktorTiltaksgjennomforingStructure";
import { TiltakstypeOgTiltaksgjennomforingPreview } from "./previews/TiltakstypeOgTiltaksgjennomforingPreview";
import { TiltakstypePreview } from "./previews/TiltakstypePreview";

export const structure = (S, context) => {
  const { currentUser } = context;
  const roleNames = currentUser.roles.map((r) => r.name);

  const deskItems = [];

  // Innhold for fagansvarlige i AV.Dir
  if (roleNames.includes("redaktor_av_dir")) {
    deskItems.push(...redaktorAvdirStructure(S, context));
    return S.list().title("Innhold for fagansvarlige i AV.Dir").items(deskItems);
  }

  // Innhold for tiltaksansvarlige
  if (roleNames.includes("redaktor-tiltaksgjennomforing")) {
    deskItems.push(...redaktorTiltaksgjennomforingStructure(S, context));
    return S.list().title("Administrasjon av tiltaksgjennomføringer").items(deskItems);
  }

  deskItems.push(...adminStructure(S, context));
  return S.list().title("Administrator").items(deskItems);
};

export const defaultDocumentNode = (S, { schemaType }) => {
  if (schemaType === "tiltaksgjennomforing") {
    return S.document().views([
      S.view.form(),
      S.view
        .component(TiltakstypeOgTiltaksgjennomforingPreview)
        .title("Forhåndsvisning av tiltaksgjennomføring"),
    ]);
  }

  if (schemaType === "tiltakstype") {
    return S.document().views([
      S.view.form(),
      S.view.component(TiltakstypePreview).title("Forhåndsvisning av tiltakstype"),
    ]);
  }
};
