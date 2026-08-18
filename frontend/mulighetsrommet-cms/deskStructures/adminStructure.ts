import { commonStructure } from "./commonStructure";

const adminStructure = (S, context) => [
  ...commonStructure(S, context),
  S.divider(),
  ...S.documentTypeListItems(),
];

export default adminStructure;
