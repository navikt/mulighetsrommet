import { GrUserAdmin } from "react-icons/gr";
import { Rule } from "@sanity/types";
import userStore from "part:@sanity/base/user";

export default {
  name: "redaktor",
  title: "Redaktør",
  type: "document",
  icon: GrUserAdmin,
  fields: [
    {
      name: "navn",
      title: "Navn",
      type: "string",
      validation: (Rule: Rule) => Rule.required().min(2).max(200),
      initialValue: async () => {
        const { name } = await userStore.getCurrentUser();
        return name;
      },
    },
    {
      name: "epost",
      title: "NAV-epost",
      type: "slug",
      validation: (Rule: Rule) => Rule.required(),
      initialValue: async () => {
        const { email } = await userStore.getCurrentUser();
        return { _type: "slug", current: email };
      },
    },
    {
      name: "enhet",
      title: "NAV-enhet",
      description: "Her velger du hvilken NAV-enhet du tilhører",
      type: "string",
      validation: (Rule: Rule) => Rule.required().min(2).max(200),
    },
  ],
  preview: {
    select: {
      title: "navn",
      subtitle: "enhet",
    },
  },
};
