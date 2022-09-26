import S from "@sanity/desk-tool/structure-builder";
import userStore from "part:@sanity/base/user";
import adminStructure from "./adminStructure";
import redaktorAvdirStructure from "./redaktorAvdirStructure";
import { TiltakstypeOgTiltaksgjennomforingPreview } from "./previews/TiltakstypeOgTiltaksgjennomforingPreview";

export default () =>
  userStore
    .getCurrentUser()
    .then((user) => user.roles)
    .then((roles) => {
      const roleNames = roles.map((r) => r.name);

      const deskItems = [];

      if (roleNames.includes("administrator")) {
        deskItems.push(...adminStructure);
        return S.list().title("Adminstrator").items(deskItems);
      }

      // Innhold for fagansvarlige i AV.Dir
      if (roleNames.includes("redaktor_av_dir")) {
        deskItems.push(...redaktorAvdirStructure);
        return S.list()
          .title("Innhold for fagansvarlige i AV.Dir")
          .items(deskItems);
      }

      return S.list()
        .title("Innhold")
        .items([...adminStructure]);
    });

export const getDefaultDocumentNode = ({ schemaType }) => {
  if (schemaType === "tiltaksgjennomforing") {
    return S.document().views([
      S.view.form(),
      S.view
        .component(TiltakstypeOgTiltaksgjennomforingPreview)
        .title("Forhåndsvisning"),
    ]);
  }
};
