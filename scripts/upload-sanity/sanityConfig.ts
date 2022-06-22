require("dotenv").config();
const sanityClient = require("@sanity/client");
const colors = require("colors");

const projectId = process.env.SANITY_PROJECT_ID;
const dataset = process.env.SANITY_DATASET;

if (!projectId) {
  console.log(
    colors.red(
      "Ingen projectId satt i .env for nøkkel 'SANITY_PROJECT_ID'. Sett en projectId for å gå videre."
    )
  );
  process.exit(1);
}

if (!dataset) {
  console.log(
    colors.red(
      "Ingen dataset satt i .env for nøkkel 'SANITY_DATASET'. Sett et dataset for å gå videre."
    )
  );
  process.exit(1);
}

const config = {
  projectId: process.env.SANITY_PROJECT_ID,
  dataset: process.env.SANITY_DATASET,
  apiVersion: "2022-06-03", // use current UTC date - see "specifying API version"!
  token: process.env.SANITY_TOKEN, // or leave blank for unauthenticated usage
  useCdn: false, // `false` if you want to ensure fresh data
};

console.log(
  `Setter opp Sanity-Client med følgende config\nProject-id: ${config.projectId}\nDataset: ${config.dataset}`
);

const client = sanityClient(config);

export { client };
