import { GrLink } from "react-icons/gr";

export default {
    name: "regelverklenke",
    title: "Regelverk lenke",
    type: "document",
    icon: GrLink,
    fields: [
        {
            name: "regelverkurl",
            title: "Regelverk URL",
            type: "url",
            placeholder: "https://www...",
        },
        {
            name: "regelverkLenkeNavn",
            title: "Navn til lenke",
            description: "Hvilket navn skal vises i lenken?",
            type: "string",
            validation: (Rule) =>
                Rule.custom((field, context) =>
                    context.document.regelverkLenke && field === undefined
                        ? "Dette feltet kan ikke være tomt."
                        : true
                ),
            hidden: ({ document }) => !document?.regelverkurl,
        },
    ],
    preview: {
        select: {
            title: "regelverkLenkeNavn",
        },
    },
};
