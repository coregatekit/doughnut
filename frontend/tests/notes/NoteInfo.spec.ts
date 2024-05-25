import { flushPromises } from "@vue/test-utils";
import NoteInfoBar from "@/components/notes/NoteInfoBar.vue";
import makeMe from "../fixtures/makeMe";
import helper from "../helpers";

const stubResponse = {
  reviewPoint: makeMe.aReviewPoint.please(),
  note: makeMe.aNoteRealm.please(),
};

describe("note info", () => {
  it("should render values", async () => {
    helper.managedApi.restNoteController.getNoteInfo = vi
      .fn()
      .mockResolvedValue(stubResponse);
    const wrapper = helper
      .component(NoteInfoBar)
      .withProps({
        noteId: 123,
      })
      .mount();
    await flushPromises();
    expect(wrapper.findAll(".statistics-value")).toHaveLength(3);
    expect(helper.managedApi.restNoteController.getNoteInfo).toBeCalledWith(
      123,
    );
  });
});
