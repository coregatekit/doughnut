import { routerScopeGuard } from '@/routes/relative_routes'

describe('router guards', () => {
  var alert;
  var guard;
  var next;

  beforeEach(async () => {
    alert = jest.fn()
    guard = routerScopeGuard("repeat", ["repeat-quiz"], async ()=>alert())
    next = jest.fn()
  });
  test('when in repeat, go to nested noteShow', async () => {
    await guard({name: 'noteShow', params: {noteId: 3}}, {name: 'repeat'}, next)
    expect(next).toHaveBeenCalledWith({name: 'repeat-noteShow', params: {noteId: 3}});
  })

  test('when in repeat, and going to already nested route', async () => {
    await guard({name: 'repeat-noteShow', params: {noteId: 3}}, {name: 'repeat'}, next)
    expect(next).toHaveBeenCalledWith();
  })

  test('when in repeat, and going to a route that doesnot have nested route', async () => {
    await guard({name: 'initial', params: {noteId: 3}}, {name: 'repeat'}, next)
    expect(next).toHaveBeenCalledWith();
  })

  test('when in repeat-quiz, and going to a note route', async () => {
    await guard({name: 'noteShow', params: {noteId: 2}}, {name: 'repeat-quiz'}, next)
    expect(alert).toHaveBeenCalledTimes(1);
    expect(next).toHaveBeenCalledTimes(0);
  })

  test('when in repeat-quiz, and going to reviews', async () => {
    await guard({name: 'reviews'}, {name: 'repeat-quiz'}, next)
    expect(alert).toHaveBeenCalledTimes(0);
    expect(next).toHaveBeenCalledTimes(1);
  })

})