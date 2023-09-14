/**
 * THIS SCRIPT DELETES DATA!
 *
 * To use this script:
 * 1. Put this script in your studio folder
 * 2. Set FIELD_TO_REMOVE to the field you want to remove (using dot notation to drill down)
 * 3. Set TOKEN to a token with editing permission
 * 4. Optionally, modify the GROQ filter in fetchDocuments() to further restrict the query, if needed
 * 5. Run `sanity dataset export` if you want to backup your dataset before modifying a bunch of documents
 * 6. Run `sanity exec removeField.js` in your terminal to delete the field from all matched documents
 */

import { createClient } from "@sanity/client";



const FIELD_TO_REMOVE = 'kontaktinfoArrangor'
const TOKEN = '';
const DATASET = 'test';



const client = createClient({
  projectId: "xegcworx",
  dataset: DATASET,
  useCdn: false,
  apiVersion: "2021-10-21",
  token: TOKEN,
});

const fetchDocuments = () => client.fetch(`*[_type == 'tiltaksgjennomforing' && ${FIELD_TO_REMOVE} != null][0...10] {_id, _rev}`)

const buildPatches = docs => docs.map(doc => ({
  id: doc._id,
  patch: {
    unset: [FIELD_TO_REMOVE],
    ifRevisionID: doc._rev
  }
}));

const createTransaction = patches =>
  patches.reduce((tx, patch) => tx.patch(patch.id, patch.patch), client.transaction())

const commitTransaction = tx => tx.commit()

const editNextBatch = async () => {
  const documents = await fetchDocuments()
  const patches = buildPatches(documents)

  if (patches.length === 0) {
    console.log('No more documents to unset!')
    return null
  }

  console.log(
    `Editing batch:\n %s`,
    patches.map(patch => `${patch.id} => ${JSON.stringify(patch.patch)}`).join('\n')
  )
  const transaction = createTransaction(patches)
  await commitTransaction(transaction)
  return editNextBatch()
}

editNextBatch().catch(err => {
  console.error(err)
  process.exit(1)
})
