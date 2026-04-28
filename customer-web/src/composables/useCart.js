import { ref, computed } from 'vue'

const STORAGE_KEY = 'cart'

export function useCart() {
  const cartItems = ref([])

  function load() {
    try { cartItems.value = JSON.parse(localStorage.getItem(STORAGE_KEY) || '[]') }
    catch { cartItems.value = [] }
  }

  function save() {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(cartItems.value))
    window.dispatchEvent(new Event('cart-changed'))
  }

  function addItem(menu) {
    if (menu.isSoldOut) return
    load()
    const id = menu.menuId || menu.id
    const idx = cartItems.value.findIndex(i => i.menuId === id)
    if (idx >= 0) {
      cartItems.value[idx].quantity++
    } else {
      cartItems.value.push({ menuId: id, name: menu.name, price: menu.price, quantity: 1, imageUrl: menu.imageUrl || '' })
    }
    save()
  }

  function changeQty(item, delta) {
    const idx = cartItems.value.findIndex(i => i.menuId === item.menuId)
    if (idx < 0) return
    cartItems.value[idx].quantity += delta
    if (cartItems.value[idx].quantity <= 0) cartItems.value.splice(idx, 1)
    save()
  }

  function removeItem(item) {
    cartItems.value = cartItems.value.filter(i => i.menuId !== item.menuId)
    save()
  }

  function clear() {
    cartItems.value = []
    save()
  }

  const totalQty = computed(() => cartItems.value.reduce((s, i) => s + i.quantity, 0))
  const totalPrice = computed(() => cartItems.value.reduce((s, i) => s + i.price * i.quantity, 0))
  const itemNames = computed(() => {
    const names = cartItems.value.map(i => i.name)
    if (names.length <= 2) return names.join(', ')
    return `${names[0]} 외 ${names.length - 1}건`
  })

  load()

  return { cartItems, load, save, addItem, changeQty, removeItem, clear, totalQty, totalPrice, itemNames }
}
