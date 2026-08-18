import adminStructure from "./adminStructure";
import redaktorAvdirStructure from "./redaktorAvdirStructure";
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

  deskItems.push(...adminStructure(S, context));
  return S.list().title("Administrator").items(deskItems);
};

export const defaultDocumentNode = (S, { schemaType }) => {
  if (schemaType === "tiltakstype") {
    return S.document().views([
      S.view.form(),
      S.view.component(TiltakstypePreview).title("Forhåndsvisning av tiltakstype"),
    ]);
  }
};
