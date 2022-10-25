import { route } from "part:@sanity/base/router";
import Samtykke from "./Samtykke";

export default {
  title: "Samtykke for redaktører",
  name: "samtykke",
  router: route("/:selectedDocumentId"),
  component: Samtykke,
};
