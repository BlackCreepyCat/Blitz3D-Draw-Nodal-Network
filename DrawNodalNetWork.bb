; -----------------------------------------------
; Name : Draw Nodal network V2 with root creation and multi-selection
; Date : (C)2025
; Site : https://github.com/BlackCreepyCat
; -----------------------------------------------


Graphics3D 1920,1080,0,2
SetBuffer BackBuffer()



; Définition du type NodalNet
Type NodalNet
    Field NType%        ; 0 = Point de liaison, 1 = Valeur
    Field Radius%       ; Rayon du cercle pour les nodes de valeur
    Field Parent.NodalNet ; Node parent
    Field Value#        ; Valeur du node
    Field Px%          ; Position X
    Field Py%          ; Position Y
    Field Caption$     ; Légende
    Field TargetX#     ; Position X cible pour le déplacement doux
    Field TargetY#     ; Position Y cible pour le déplacement doux
End Type

; Type pour gérer la liste des nodes sélectionnés
Type SelectedNode
    Field Node.NodalNet
End Type

; Variables globales pour la caméra
Global cam_zoom# = 1.0
Global cam_target_zoom# = 1.0 ; Zoom cible pour le zoom doux
Global cam_x# = 0
Global cam_y# = 0
Global cam_target_x# = 0    ; Position X cible pour la caméra
Global cam_target_y# = 0    ; Position Y cible pour la caméra
Global mouse_down = False
Global last_mx%, last_my%
Global selected_node.NodalNet = Null ; Node en cours de déplacement

; Création du réseau de test
Function CreateTestNetwork()
    root.NodalNet = CreateNode(0, 0, 0, 30, "ROOT : " + Rand(100,300), Null)
   
    For i = 1 To 15
        angle# = i * 24
        dist# = 50 + i * 20
        node.NodalNet = CreateNode(1, root\Px + Cos(angle) * dist, root\Py + Sin(angle) * dist, Rand(50,100), "NODE : " + Rand(100,300), root)
    Next
End Function

; Function pour créer un node
Function CreateNode.NodalNet(NType%, Px%, Py%, Value#, Caption$, Parent.NodalNet)
    Node.NodalNet = New NodalNet
    Node\NType = NType%
    Node\Px = Px%
    Node\Py = Py%
    Node\Value# = Value#
    Node\Radius% = Node\Value# / 2
    Node\Caption$ = Caption$
    Node\TargetX = Node\Px
    Node\TargetY = Node\Py
    Node\Parent = Parent
    Return Node
End Function

; Vérifie si un node a des enfants
Function HasChildren(node.NodalNet)
    For child.NodalNet = Each NodalNet
        If child\Parent = node Then Return True
    Next
    Return False
End Function

Function DeleteNodeAndChildren(node.NodalNet)
    For child.NodalNet = Each NodalNet
        If child\Parent = node Then DeleteNodeAndChildren(child)
    Next
    Delete node
End Function

; Fonction pour vérifier si la souris est sur un node
Function CheckNodeUnderMouse.NodalNet()
    mx# = (MouseX() - GraphicsWidth()/2) / cam_zoom - cam_x
    my# = (MouseY() - GraphicsHeight()/2) / cam_zoom - cam_y
   
    For node.NodalNet = Each NodalNet
        dx# = node\Px - mx
        dy# = node\Py - my
        dist# = Sqr(dx*dx + dy*dy)
        If node\NType = 1 Then
            If dist < node\Radius Then Return node
        ElseIf node\NType = 0 Then
            If dist < node\Radius Then Return node
        EndIf
    Next
    Return Null
End Function

; Fonction pour vérifier si un node est déjà sélectionné
Function IsNodeSelected(node.NodalNet)
    For sel.SelectedNode = Each SelectedNode
        If sel\Node = node Then Return True
    Next
    Return False
End Function

Function ConnectSelectedNodes()
    Local first_node.NodalNet = Null
    Local second_node.NodalNet = Null
    For sel.SelectedNode = Each SelectedNode
        If first_node = Null Then
            first_node = sel\Node
        ElseIf second_node = Null Then
            second_node = sel\Node
            Exit
        EndIf
    Next
    If first_node <> Null And second_node <> Null Then
        If first_node\NType = 0 And second_node\NType = 1 Then
            second_node\Parent = first_node ; Root -> Valeur
        ElseIf first_node\NType = 1 And second_node\NType = 0 Then
            first_node\Parent = second_node ; Valeur -> Root
        Else
            second_node\Parent = first_node ; Sinon, second vers premier
        EndIf
    EndIf
End Function

; Fonction pour déplacer un node et uniquement ses enfants de type valeur
Function MoveNodeAndChildren(node.NodalNet, dx#, dy#)
    node\TargetX = node\TargetX + dx
    node\TargetY = node\TargetY + dy
   
    For child.NodalNet = Each NodalNet
        If child\Parent = node And child\NType = 1 Then
            MoveNodeAndChildren(child, dx, dy)
        EndIf
    Next
End Function

; Fonction pour sauvegarder les nodes
Function SaveNetwork(filename$)
    file = WriteFile(filename$)
    If file = 0 Then Return False
   
    count = 0
    For node.NodalNet = Each NodalNet
        count = count + 1
    Next
    WriteInt(file, count)
   
    For node.NodalNet = Each NodalNet
        WriteInt(file, node\NType)
        WriteInt(file, node\Radius)
        WriteFloat(file, node\Value)
        WriteInt(file, node\Px)
        WriteInt(file, node\Py)
        WriteString(file, node\Caption)
        parent_index = -1
        index = 0
        For n.NodalNet = Each NodalNet
            If n = node\Parent Then
                parent_index = index
                Exit
            EndIf
            index = index + 1
        Next
        WriteInt(file, parent_index)
    Next
   
    CloseFile(file)
    Return True
End Function

; Fonction pour charger les nodes
Function LoadNetwork(filename$)
    For node.NodalNet = Each NodalNet
        Delete node
    Next
    For sel.SelectedNode = Each SelectedNode
        Delete sel
    Next
   
    file = ReadFile(filename$)
    If file = 0 Then Return False
   
    count = ReadInt(file)
   
    For i = 1 To count
        node.NodalNet = New NodalNet
        node\NType = ReadInt(file)
        node\Radius = ReadInt(file)
        node\Value = ReadFloat(file)
        node\Px = ReadInt(file)
        node\Py = ReadInt(file)
        node\Caption = ReadString(file)
        node\TargetX = node\Px
        node\TargetY = node\Py
        ReadInt(file)
    Next
   
    SeekFile(file, 4)
   
    For node.NodalNet = Each NodalNet
        ReadInt(file)
        ReadInt(file)
        ReadFloat(file)
        ReadInt(file)
        ReadInt(file)
        ReadString(file)
        parent_index = ReadInt(file)
       
        If parent_index >= 0 Then
            index = 0
            For n.NodalNet = Each NodalNet
                If index = parent_index Then
                    node\Parent = n
                    Exit
                EndIf
                index = index + 1
            Next
        Else
            node\Parent = Null
        EndIf
    Next
   
    CloseFile(file)
    Return True
End Function

; Fonction pour dessiner le réseau
Function DrawNetwork()
    For node.NodalNet = Each NodalNet
        screen_x# = (node\Px + cam_x) * cam_zoom + GraphicsWidth()/2
        screen_y# = (node\Py + cam_y) * cam_zoom + GraphicsHeight()/2
       
        If node\Parent <> Null Then
            parent_x# = (node\Parent\Px + cam_x) * cam_zoom + GraphicsWidth()/2
            parent_y# = (node\Parent\Py + cam_y) * cam_zoom + GraphicsHeight()/2

			Gui_Line(screen_x, screen_y, parent_x, parent_y, 100,100,100)
        EndIf
       
        If node\NType = 0 Then
			Gui_Oval(screen_x-(node\Radius/2) , screen_y-(node\Radius/2) , node\Radius , node\Radius , 1, 255,0,0)
			
            If IsNodeSelected(node) Then
                radius_scaled# = 15 * cam_zoom

				Gui_Oval(screen_x - radius_scaled, screen_y - radius_scaled, radius_scaled * 2, radius_scaled * 2 , 1, 139,0,0)
            EndIf

			Gui_Text(screen_x, screen_y+10, node\Caption, 255,255,255, 1 , True)

        Else
            radius_scaled# = node\Radius * cam_zoom
			
            If IsNodeSelected(node) Then
				Gui_Oval(screen_x - radius_scaled, screen_y - radius_scaled, radius_scaled * 2, radius_scaled * 2 , 1, 0,255,0)
            ElseIf HasChildren(node) Then
				Gui_Oval(screen_x - radius_scaled, screen_y - radius_scaled, radius_scaled * 2, radius_scaled * 2 , 1, 255,165,0)
            Else
				Gui_Oval(screen_x - radius_scaled, screen_y - radius_scaled, radius_scaled * 2, radius_scaled * 2 , 1, 55,165,250)
            EndIf

			Gui_Oval(screen_x + 2 - radius_scaled, screen_y + 2 - radius_scaled, radius_scaled * 2 - 4, radius_scaled * 2 - 4 , 1, 50,50,50)

			Gui_Text(screen_x, screen_y, Int(node\Value), 255,255,255, 1 , True)
			Gui_Text(screen_x, screen_y + radius_scaled + 5, node\Caption$, 255,255,255, 1 , True)

        EndIf
    Next
End Function

; Fonction pour transformer un node de valeur en root tout en gardant ses enfants
Function TransformValueToRoot()
    For sel.SelectedNode = Each SelectedNode
        If sel\Node\NType = 1 Then ; Vérifie si c'est un node de valeur
            sel\Node\NType = 0     ; Transforme en root
            sel\Node\Value = 30    ; Définit une valeur fixe pour les roots
            sel\Node\Radius = sel\Node\Value / 2 ; Met à jour le rayon
            sel\Node\Caption = "ROOT : " + Rand(100,300) ; Change le caption
        EndIf
    Next
End Function

; Nouvelle fonction pour changer la valeur d'un node sélectionné
Function ChangeNodeValue(Value#)
    For sel.SelectedNode = Each SelectedNode
        If sel\Node\NType = 1 Then ; Ne change que les nodes de valeur
            sel\Node\Value = Value# ; Nouvelle valeur aléatoire entre 50 et 100
            sel\Node\Radius = sel\Node\Value / 2 ; Met à jour le rayon
        EndIf
    Next
End Function

; Gfx Function (them them for fastimage)
Function Gui_Line(Px%, Py%, Sx%, Sy%, R%, G%, B%)
	Color R%, G%, B%
	Line(Px, Py, Sx, Sy)
End Function

Function Gui_Oval(Px, Py, Sx, Sy, Fill%, R%, G%, B%, Style% = 0)
    Select Style%
           
        ; Simple
        Case 0
            Color R%, G%, B% : Oval(Px, Py, Sx, Sy, Fill%)
           
        ; Flat Border
        Case 1
            Color R%/2, G%/2, B%/2 : Oval(Px, Py, Sx, Sy, Fill%)
            Color 100, 100, 100 : Oval(Px, Py, Sx, Sy, 0)
           
            Color R%, G%, B% : Oval(Px + (Sx/4), Py + (Sy/4), Sx/2, Sy/2, Fill%)
           
    End Select
End Function

Function Gui_Text(Px, Py, Caption$, R% = 255, G% = 255, B% = 255, Style% = 1 , Centered% = 0)
    Select Style%
        Case 1
            Color R%, G%, B% : Text(Px, Py, Caption$, Centered% , Centered% )
    End Select
End Function



; Fonction principale
CreateTestNetwork()

While Not KeyHit(1)
    Cls
   
    ; Gestion du zoom avec la molette
    zoom_change = MouseZSpeed()
    If zoom_change > 0 Then cam_target_zoom = cam_target_zoom * 1.1
    If zoom_change < 0 Then cam_target_zoom = cam_target_zoom / 1.1
    If cam_target_zoom < 0.1 Then cam_target_zoom = 0.1
   
    If KeyHit(211) Then ; Touche Suppr
        For sel.SelectedNode = Each SelectedNode
            DeleteNodeAndChildren(sel\Node)
            Delete sel
        Next
    EndIf

    If MouseDown(1) Then
        If Not mouse_down Then
            mouse_down = True
            last_mx = MouseX()
            last_my = MouseY()
            selected_node = CheckNodeUnderMouse()
           
            If selected_node <> Null Then
                If KeyDown(42) Or KeyDown(54) Then ; Shift gauche ou droit
                    If Not IsNodeSelected(selected_node) Then
                        sel.SelectedNode = New SelectedNode
                        sel\Node = selected_node
                    EndIf
                Else
                    ; Si Shift n'est pas enfoncé, réinitialiser la sélection
                    For sel.SelectedNode = Each SelectedNode
                        Delete sel
                    Next
                    sel.SelectedNode = New SelectedNode
                    sel\Node = selected_node
                EndIf
            Else
                ; Si aucun node n'est sous la souris, désélectionner tout
                For sel.SelectedNode = Each SelectedNode
                    Delete sel
                Next
            EndIf
        Else
            If selected_node <> Null Then
                mx# = (MouseX() - GraphicsWidth()/2) / cam_zoom - cam_x
                my# = (MouseY() - GraphicsHeight()/2) / cam_zoom - cam_y
                dx# = mx - selected_node\TargetX
                dy# = my - selected_node\TargetY
                If KeyDown(29) And selected_node\NType = 0 Then ; Ctrl gauche + root node
                    MoveNodeAndChildren(selected_node, dx, dy)
                Else
                    selected_node\TargetX = mx
                    selected_node\TargetY = my
                EndIf
            Else
                cam_target_x = cam_target_x + (MouseX() - last_mx) / cam_zoom
                cam_target_y = cam_target_y + (MouseY() - last_my) / cam_zoom
                last_mx = MouseX()
                last_my = MouseY()
            EndIf
        EndIf
    Else
        mouse_down = False
        selected_node = Null
    EndIf
   
    ; Bouton du milieu : création d'un root node
    If MouseHit(2) Then
        new_root.NodalNet = CreateNode(0, (MouseX() - GraphicsWidth()/2) / cam_zoom - cam_x, (MouseY() - GraphicsHeight()/2) / cam_zoom - cam_y, 30, "ROOT : " + Rand(100,300), Null)
       
        If KeyDown(42) Or KeyDown(54) Then
            sel.SelectedNode = New SelectedNode
            sel\Node = new_root
        Else
            For sel.SelectedNode = Each SelectedNode
                Delete sel
            Next
            sel.SelectedNode = New SelectedNode
            sel\Node = new_root
        EndIf
    EndIf
   
    ; Bouton droit : création d'un node de valeur si au moins un node est sélectionné
    If MouseHit(3) Then
        For sel.SelectedNode = Each SelectedNode
            new_node.NodalNet = CreateNode(1, (MouseX() - GraphicsWidth()/2) / cam_zoom - cam_x, (MouseY() - GraphicsHeight()/2) / cam_zoom - cam_y, Rnd(50,100), "NODE : " + Rand(100,300), sel\Node)
            Exit ; Crée un seul node pour le premier sélectionné
        Next
    EndIf
   
    ; Touche "C" pour connecter les nodes sélectionnés
    If KeyHit(46) Then ; Touche "C"
        ConnectSelectedNodes()
    EndIf
   
    ; Touche "T" pour transformer un node de valeur en root
    If KeyHit(20) Then ; Touche "T"
        TransformValueToRoot()
    EndIf
   
    ; Touche "V" pour changer la valeur d'un node sélectionné
    If KeyHit(47) Then ; Touche "V"
        ChangeNodeValue(Rnd(50,300))
    EndIf
   
    ; Interpolation des positions pour les nodes
    For node.NodalNet = Each NodalNet
        If node\TargetX = 0 And node\TargetY = 0 Then
            node\TargetX = node\Px
            node\TargetY = node\Py
        EndIf
        node\Px = node\Px + (node\TargetX - node\Px) * 0.1
        node\Py = node\Py + (node\TargetY - node\Py) * 0.1
    Next
   
    ; Interpolation pour la caméra (position et zoom)
    cam_x = cam_x + (cam_target_x - cam_x) * 0.1
    cam_y = cam_y + (cam_target_y - cam_y) * 0.1
    cam_zoom = cam_zoom + (cam_target_zoom - cam_zoom) * 0.1

   
    ; Appel de la fonction pour dessiner le réseau
    DrawNetwork()
   
    ; Sauvegarde et chargement
    If KeyHit(31) Then SaveNetwork("network.txt") ; S
    If KeyHit(38) Then LoadNetwork("network.txt") ; L
   
    Color(255,255,255)
    Text 10,10, "Zoom: "+cam_zoom
    Text 10,25, "Molette: zoom | Gauche: déplacer/sélectionner | Shift+Gauche: multisélection | Milieu: root | Droit: node | C: connecter | Ctrl+Gauche: déplacer valeurs | T: valeur -> root | V: changer valeur"

    Flip
Wend

End
